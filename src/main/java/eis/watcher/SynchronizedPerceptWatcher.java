package eis.watcher;

import eis.EISAdapter;
import eis.exceptions.PerceiveException;
import eis.iilang.EnvironmentState;
import eis.iilang.Percept;
import eis.agent.AgentContainer;
import eis.percepts.attachments.AttachmentBuilder;
import eis.percepts.containers.InvalidPerceptCollectionException;
import eis.percepts.containers.SharedPerceptContainer;
import eis.percepts.things.Thing;
import map.Position;
import massim.eismassim.EnvironmentInterface;
import messages.Message;
import serializers.GsonInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Stopwatch;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class is responsible for polling agent percepts and updating the AgentContainer objects upon retrieval of new
 * percepts.
 */
public class SynchronizedPerceptWatcher extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger("PerceptWatcher");
    private static SynchronizedPerceptWatcher synchronizedPerceptWatcher;

    public Map<AgentContainer, Map<AgentContainer, Thing>> relPos = new HashMap<>();

    // Contain the agent containers
    private ConcurrentMap<String, AgentContainer> agentContainers;
    private SharedPerceptContainer sharedPerceptContainer;
    private EnvironmentInterface environmentInterface;

    private SynchronizedPerceptWatcher(EnvironmentInterface environmentInterface) {
        this.environmentInterface = environmentInterface;
        agentContainers = new ConcurrentHashMap<>();

        // Set the thread name
        setName("SynchronizedPerceptWatcherThread");
    }

    private synchronized void initializeAgentContainers() {
        if (environmentInterface.getAgents().isEmpty())
            throw new RuntimeException("The EnvironmentInterface has not registered any entities yet.");

        for (String agentName : environmentInterface.getAgents())
            agentContainers.put(agentName, new AgentContainer(agentName));

    }

    @Override
    public synchronized void start() {
        if (agentContainers.isEmpty())
            initializeAgentContainers();
        super.start();
    }

    public static SynchronizedPerceptWatcher getInstance() {
        if (EISAdapter.getSingleton().getEnvironmentInterface() == null)
            throw new NullPointerException("Environment Interface has not been initialized.");

        if (synchronizedPerceptWatcher == null)
            synchronizedPerceptWatcher = new SynchronizedPerceptWatcher(EISAdapter.getSingleton().getEnvironmentInterface());

        return synchronizedPerceptWatcher;
    }

    private synchronized void setSharedPerceptContainer(SharedPerceptContainer sharedPerceptContainer) {
        if (this.sharedPerceptContainer == null || sharedPerceptContainer.getStep() > this.sharedPerceptContainer.getStep())
            this.sharedPerceptContainer = sharedPerceptContainer;

        notifyAll();
    }

    private void waitForEntityConnection(String entity) {
        while (!environmentInterface.isEntityConnected(entity)) {
            LOG.warn("Not connected. Waiting for a connection.");

            // Sleep before we try again.
            try {
                Thread.sleep(500);
            } catch (InterruptedException exc) {
                exc.printStackTrace();
            }
        }
    }

    /**
     * This method requests entity perceptions.
     * The request will block if no new percepts have arrived since the last call.
     *
     * @param entity The name of the registered entity
     */
    private List<Percept> getAgentPercepts(String entity) {
        try {
            waitForEntityConnection(entity);

            LOG.info("Waiting for new Perceptions [" + entity + "]...");
            Map<String, Collection<Percept>> perceptMap = environmentInterface.getAllPercepts(entity);
            LOG.info("Received new Perceptions [" + entity + "]...");

            if (perceptMap.size() != 1)
                throw new RuntimeException("Failed to retrieve percept map. Percepts: " + perceptMap);

            return new ArrayList<>(perceptMap.getOrDefault(entity, new ArrayList<>()));

        } catch (PerceiveException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public synchronized AgentContainer getAgentContainer(String e) {
        return this.agentContainers.get(e);
    }

    @Override
    public void run() {
        // Thread pool for percept parsers
        // ExecutorService perceptListenerExecutorService = Executors.newCachedThreadPool();

        while (environmentInterface.getState() != EnvironmentState.KILLED) {


            Map<String, List<Percept>> agentPerceptUpdates = new HashMap<>();

            environmentInterface.getEntities().forEach(e -> {
                agentPerceptUpdates.put(e, getAgentPercepts(e));
            });

            try {
                // All objects that call this class should wait until percepts are updated for all entities
                synchronized (this) {
                    // Check for new perceptions & update the agents.
                    Stopwatch sw = Stopwatch.startTiming();
                    var isFirst = relPos.isEmpty();

                    agentContainers.values().forEach(a -> {


                        // DEBUGGING:

                        Map<AgentContainer, Thing> pos = new HashMap<>();

                        var iter = agentPerceptUpdates.get(a.getAgentName()).listIterator();
                        while (iter.hasNext()) {


                            var next = iter.next();
                            if (next.getName().equals("thing")) {
                                String name = "";

                                switch ( next.getParameters().get(3).toProlog())
                                {
                                    case "agent-TRG1":
                                        name = "agentA1";
                                        break;
                                    case "agent-TRG2":
                                        name = "agentA2";
                                        break;
                                    case "agent-TRG3":
                                        name = "agentA3";
                                        break;
                                }


                                if (!name.isBlank())
                                {
                                    var cont = agentContainers.get(name);
                                    var thing = Thing.ParseThing(next);
                                    if(isFirst && cont.equals(a))
                                    {
                                        cont.setCurrentLocation(thing.getPosition());
                                    }
                                    pos.put(cont, thing);
                                    iter.remove();
                                }
                            }
                        }

                        relPos.put(a, pos);


                        try {
                            a.updatePerceptions(agentPerceptUpdates.get(a.getAgentName()));
                        } catch (InvalidPerceptCollectionException e) {
                            if (e.isStartPercepts())
                                return;

                            throw e;
                        }

                        setSharedPerceptContainer(a.getSharedPerceptContainer());
                    });


                    // Agents should now update their respective maps
                    agentContainers.values().forEach(AgentContainer::updateMap);

                    // Agents should now synchronize maps.
                    agentContainers.values().forEach(AgentContainer::synchronizeMap);


                    long deltaTime = sw.stopMS();

                    // Update any consumers that are subscribed to the containers
                    agentContainers.values().forEach(Message::createAndSendAgentContainerMessage);

                    if (deltaTime > 500 && agentContainers.size() > 0)
                        LOG.warn("Step " + agentContainers.get(environmentInterface.getEntities().getFirst()).getSharedPerceptContainer().getStep() + " took " + deltaTime + " ms to process map updates and synchronization.");

                }
            } catch (InvalidPerceptCollectionException e) {
                // We want to ignore if this exception is a result of the sim-start message. (we don't need to parse that).
                // This was a bad/lazy way of handling this but I didn't want to spend too much time on this.
                if (!e.isStartPercepts())
                    throw e;
            }

        }
    }

    public synchronized SharedPerceptContainer getSharedPerceptContainer() {
        while (sharedPerceptContainer == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return sharedPerceptContainer;
    }
}
