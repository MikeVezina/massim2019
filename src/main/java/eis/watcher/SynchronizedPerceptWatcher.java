package eis.watcher;

import eis.EISAdapter;
import eis.exceptions.PerceiveException;
import eis.iilang.EnvironmentState;
import eis.iilang.Percept;
import eis.agent.AgentContainer;
import eis.percepts.containers.InvalidPerceptCollectionException;
import eis.percepts.containers.SharedPerceptContainer;
import eis.percepts.things.Thing;
import massim.eismassim.EnvironmentInterface;
import messages.Message;
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
    private ConcurrentMap<String, AgentContainer> usernameContainerMap;


    private SharedPerceptContainer sharedPerceptContainer;
    private EnvironmentInterface environmentInterface;

    private SynchronizedPerceptWatcher(EnvironmentInterface environmentInterface) {
        this.environmentInterface = environmentInterface;
        agentContainers = new ConcurrentHashMap<>();
        usernameContainerMap = new ConcurrentHashMap<>();


        // Set the thread name
        setName("SynchronizedPerceptWatcherThread");
        setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                System.out.println("Uncaught.");
                t.start();
            }
        });
    }

    public AgentContainer getContainerByUsername(String username) {
        return usernameContainerMap.get(username);
    }

    private synchronized void initializeAgentContainers() {
        if (environmentInterface.getAgents().isEmpty())
            throw new RuntimeException("The EnvironmentInterface has not registered any entities yet.");

        for (String agentName : environmentInterface.getAgents()) {
            agentContainers.put(agentName, new AgentContainer(agentName));
        }

        // Hard code agent usernames (no way to get them via EI??)
        createUser("agent-TRG1", "agentA1");
        createUser("agent-TRG2", "agentA2");
        createUser("agent-TRG3", "agentA3");
        createUser("agent-TRG4", "agentA4");
        createUser("agent-TRG5", "agentA5");
        createUser("agent-TRG6", "agentOffender1");
        createUser("agent-TRG7", "agentOffender2");
        createUser("agent-TRG8", "agentOffender3");
        createUser("agent-TRG9", "agentOffender4");
        createUser("agent-TRG10", "agentOffender5");

    }

    private void createUser(String username, String agentName) {
        if (!agentContainers.containsKey(agentName))
            return;

        usernameContainerMap.put(username, agentContainers.get(agentName));
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

    @Override
    public void interrupt() {
        super.interrupt();
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
                                String username = next.getParameters().get(3).toProlog();

                                if (usernameContainerMap.containsKey(username)) {
                                    var cont = usernameContainerMap.get(username);
                                    var thing = Thing.ParseThing(next);
                                    if (isFirst && cont.equals(a)) {
                                        cont.setCurrentLocation(thing.getPosition());
                                    }
                                    pos.put(cont, thing);
                                    iter.remove();
                                }
                            }
                        }

                        if(!pos.isEmpty())
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
        System.out.println("SynchronizedPerceptWatcher is finished execution.");
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

    public Collection<AgentContainer> getAgentContainers() {
        return agentContainers.values();
    }
}
