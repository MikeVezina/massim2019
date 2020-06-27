package eis.agent;

import eis.EISAdapter;
import eis.iilang.Identifier;
import eis.iilang.Percept;
import eis.percepts.things.Thing;
import eis.watcher.SynchronizedPerceptWatcher;
import map.AgentMap;
import map.Direction;
import messages.MQSender;
import messages.Message;
import map.MapPercept;
import eis.percepts.attachments.AttachmentBuilder;
import eis.percepts.containers.AgentPerceptContainer;
import eis.percepts.containers.SharedPerceptContainer;
import jason.asSyntax.Literal;
import massim.protocol.messages.scenario.Actions;
import map.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class contains all relevant information for a given agent. This object needs to be thread-safe / synchronized
 * as multiple threads may access the object at the same time. I.e. the thread that watches percepts and updates
 * the AgentContainer, and the thread running perception retrieval done by the agent both may access the object at
 * the same time
 */
public class AgentContainer {

    private AttachmentBuilder attachmentBuilder;
    private AgentLocation agentLocation;
    private AgentMap agentMap;
    private AgentAuthentication agentAuthentication;
    private String agentName;
    private List<Literal> currentStepPercepts;
    private AgentPerceptContainer perceptContainer;
    private Set<Position> attachedBlocks;

    private Map<Position, AgentContainer> sharedAttachments;

    private MQSender mqSender;
    private Set<Position> removedAttachments;
    private Set<Position> addedAttachments;
    private Map<AgentContainer, Position> addedConnection;
    private final Logger LOG;
    private Set<Position> previouslyDisconnected;

    public AgentContainer(String agentName) {
        this.agentName = agentName;
        this.mqSender = new MQSender(agentName);
        this.agentLocation = new AgentLocation();
        this.attachedBlocks = new HashSet<>();
        this.currentStepPercepts = new ArrayList<>();
        this.attachmentBuilder = new AttachmentBuilder(this);
        this.agentAuthentication = new AgentAuthentication(this);
        this.agentMap = new AgentMap(this);
        this.removedAttachments = new HashSet<>();
        this.addedAttachments = new HashSet<>();
        this.addedConnection = new HashMap<>();
        this.sharedAttachments = new HashMap<>();
        this.previouslyDisconnected = new HashSet<>();
        LOG = LoggerFactory.getLogger(agentName + "-Container");
        LOG.info("Created container");
    }

    public MQSender getMqSender() {
        return mqSender;
    }

    public synchronized AgentLocation getAgentLocation() {
        return agentLocation;
    }

    public synchronized Position getCurrentLocation() {
        return agentLocation.getCurrentLocation();
    }

    public synchronized void setCurrentLocation(Position p) {
        agentLocation = new AgentLocation(p);
    }

    public AgentMap getAgentMap() {
        return agentMap;
    }

    public AgentAuthentication getAgentAuthentication() {
        return agentAuthentication;
    }

    public String getAgentName() {
        return agentName;
    }

    /**
     * This method needs to be lightweight and should only be responsible for updating the percept container. Do not call any
     * listeners or perform any GUI updates.
     *
     * @param percepts The current step percepts for this agent.
     */
    public synchronized void updatePerceptions(List<Percept> percepts) {
        this.currentStepPercepts = percepts.stream().map(EISAdapter::perceptToLiteral).collect(Collectors.toList());

        if (this.currentStepPercepts.size() != percepts.size()) {
            System.out.println("There may be an issue with async mapping.");
            throw new NullPointerException("Issues with async mapping. Actual: " + currentStepPercepts.size() + " vs. Expected: " + percepts.size());
        }

        // Create a new percept container for this step.
        perceptContainer = AgentPerceptContainer.parsePercepts(percepts);

        handleLastAction();

        notifyAll(); // Notify any agents that are waiting for perceptions

        Message.createAndSendNewStepMessage(mqSender, perceptContainer.getSharedPerceptContainer().getStep());


    }

    public synchronized Position relativeToAbsoluteLocation(Position relative) {
        return getCurrentLocation().add(relative);
    }

    public synchronized Position absoluteToRelativeLocation(Position absolute) {
        return absolute.subtract(getCurrentLocation());
    }

    private synchronized void handleLastAction() {
        // Update the location
        updateLocation();

        checkRotation();

        updateAttachedBlocks();

    }

    /**
     * Adds the successful attach (or connection) action block to our attached blocks.
     */
    private void updateAttachedBlocks() {

        // Clear any previous shared or removed attachments.
        removedAttachments.clear();
        addedAttachments.clear();
        sharedAttachments.clear();
        addedConnection.clear();
        
        removedAttachments.addAll(previouslyDisconnected);
        previouslyDisconnected.clear();

        if(!perceptContainer.getLastActionResult().equals("success"))
            return;

        boolean isAttach = getAgentPerceptContainer().getLastAction().equals("attach");
        boolean isDetach = getAgentPerceptContainer().getLastAction().equals("detach");
        boolean isConnect = getAgentPerceptContainer().getLastAction().equals("connect");
        boolean isDisconnect = getAgentPerceptContainer().getLastAction().equals("disconnect");

        if(isAttach || isDetach)
        {
            var directionParameterStr = perceptContainer.getLastActionParams().get(0).toProlog();
            var direction = Utils.DirectionStringToDirection(directionParameterStr);

            if(direction == null)
                throw new RuntimeException("Attach last parameter is invalid: " + directionParameterStr);

            if(isAttach)
            {
                checkNewSharedAttachments(direction.getPosition());
                addedAttachments.add(direction.getPosition());
            }
            else
                this.removedAttachments.add(direction.getPosition());
        }

        if(isConnect)
        {
            var usernameContainer = SynchronizedPerceptWatcher.getInstance().getContainerByUsername(perceptContainer.getLastActionParams().get(0).toProlog());
            var xString = perceptContainer.getLastActionParams().get(1).toProlog();
            var yString = perceptContainer.getLastActionParams().get(2).toProlog();
            var position = new Position(Integer.parseInt(xString), Integer.parseInt(yString));

            this.addedConnection.put(usernameContainer, position);

        }

        // Disconnect is used to disconnect an agent from a block. Params 2,3 is the entity being disconnected
        if(isDisconnect)
        {
            var xReqString = Integer.parseInt(perceptContainer.getLastActionParams().get(0).toProlog());
            var yReqString = Integer.parseInt(perceptContainer.getLastActionParams().get(1).toProlog());
            var xAgent = Integer.parseInt(perceptContainer.getLastActionParams().get(2).toProlog());
            var yAgent = Integer.parseInt(perceptContainer.getLastActionParams().get(3).toProlog());

            var agent = this.agentAuthentication.findAgentByRelativePosition(xAgent, yAgent);
            this.removedAttachments.add(new Position(xAgent, yAgent));

            if(agent == null)
            {
                LOG.error("What's going on here?");
                return;
            }

            LOG.info("Disconnecting from: " + agent.getAgentName());

            var abs = this.relativeToAbsoluteLocation(new Position(xReqString, yReqString));
            abs = this.getAgentAuthentication().translateToAgent(agent, abs);
            agent.setDisconnected(abs);
        }

    }

    /**
     * Called when another agent disconnects one of our shared connected blocks.
     * Called during the disconnect action handler
     * @param abs The absolute value of the block
     */
    private synchronized void setDisconnected(Position abs) {
        var rel = absoluteToRelativeLocation(abs);
        this.previouslyDisconnected.add(rel);
        LOG.info("Received disconnect at rel: " + rel);
    }

    private synchronized void checkRotation() {
        if (perceptContainer.getLastAction().equals(Actions.ROTATE) && perceptContainer.getLastActionResult().equals("success")) {
            String rotationLiteral = ((Identifier) perceptContainer.getLastActionParams().get(0)).getValue();
            System.out.println(agentName + ": Step " + getCurrentStep() + " performed rotation. " + perceptContainer.getLastAction() + " + " + perceptContainer.getLastActionResult());

            Rotation rotation = Rotation.getRotation(rotationLiteral);
            rotate(rotation);
        }
    }

    /**
     * This method allows us to check which attachments are still attached.
     * This also adds any attachments we didn't previously know about (i.e. those locations
     * with the attached perceptions with no surrounding connected entities)
     */
    public synchronized void updateAttachments() {
        // attachmentBuilder.getAttachments must be run before clearing the attached blocks.
        // The builder has a dependency on the current agent's attached blocks.
        Set<Position> newAttachedPositions = attachmentBuilder.getAttachments();

        this.attachedBlocks.clear();
        this.attachedBlocks.addAll(newAttachedPositions);
    }

    public synchronized Set<MapPercept> getAttachedPercepts() {
        // Map attached blocks to absolute positions and then map to MapPercepts
        return attachedBlocks.stream().map(this::relativeToAbsoluteLocation).map(p -> agentMap.getMapPercept(p)).collect(Collectors.toSet());
    }

    private synchronized void updateLocation() {
        if (perceptContainer.getLastAction().equals(Actions.MOVE) && perceptContainer.getLastActionResult().equals("success")) {
            String directionIdentifier = ((Identifier) perceptContainer.getLastActionParams().get(0)).getValue();

            System.out.println(agentName + ": Step " + getCurrentStep() + " performed movement. " + perceptContainer.getLastAction() + " + " + perceptContainer.getLastActionResult());
            try {
                agentLocation.updateAgentLocation(Utils.DirectionStringToDirection(directionIdentifier));
            } catch (NullPointerException n) {
                System.out.println(agentName + " encountered movement error on step " + getCurrentStep());
                throw n;
            }
        } else {
            System.out.println(agentName + ": Step " + getCurrentStep() + " did not perform any movement. " + perceptContainer.getLastAction() + " + " + perceptContainer.getLastActionResult());
        }

        var debugLocations = getDebuggingLocations();
        if(debugLocations != null)
        {
            // Get our absolute location
            var expectedThing = debugLocations.get(this);

            if(expectedThing == null)
            {
                System.out.println("Why is our debug value null?");
            }
            else {
                if (!agentLocation.getCurrentLocation().equals(expectedThing.getPosition())) {
                    System.out.println("debugger: positions are incorrect.");
                }
            }


        }

    }

    /**
     * Only works when debugging. Checks that our location updates are correct. Changes to the massim server need to be made to provide agents with their abs. locations.
     * @return the hashmap of agent containers to their corresponding thing perceptions
     */
    Map<AgentContainer, Thing> getDebuggingLocations()
    {
        var perceptWatcher = SynchronizedPerceptWatcher.getInstance();
        if(perceptWatcher.relPos != null && !perceptWatcher.relPos.isEmpty() && perceptWatcher.relPos.containsKey(this))
            return SynchronizedPerceptWatcher.getInstance().relPos.get(this);

        return null;
    }

    public synchronized long getCurrentStep() {
        return getAgentPerceptContainer().getSharedPerceptContainer().getStep();
    }

    public synchronized List<Literal> getCurrentPerceptions() {
        if (this.currentStepPercepts == null) {
            long startWaitTime = System.nanoTime();
            try {
                wait();
                long deltaWaitTime = (System.nanoTime() - startWaitTime) / 1000000;
                if (deltaWaitTime > 500)
                    System.out.println("Thread " + Thread.currentThread().getName() + " waited " + deltaWaitTime + " ms for perceptions.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return this.currentStepPercepts;
    }

    private synchronized void checkNewSharedAttachments(Position position) {

        // Check to see if any other agents attach the block
        for (AuthenticatedAgent auth : this.getAgentAuthentication().getAuthenticatedAgents()) {
            var agentPosition = auth.getAgentContainer().getAgentLocation();

            for (Position blockPosition : auth.getAgentContainer().getPreviouslyAddedAttachments()) {
                Position absBlockPosition = this.getAgentAuthentication().translateToAgent(auth.getAgentContainer(), agentPosition.getCurrentLocation().add(blockPosition));
                Position relBlock = this.absoluteToRelativeLocation(absBlockPosition);

                if (position.equals(relBlock))
                {
                    System.out.println("Agent " + auth.getAgentContainer().getAgentName() + " has already attached this block");
                    this.sharedAttachments.put(position, auth.getAgentContainer());
                }
            }
        }
    }

    public synchronized Map<Position, AgentContainer> getSharedAttachments() {
        return sharedAttachments;
    }

    public synchronized boolean hasAttachedPercepts() {
        return !attachedBlocks.isEmpty();
    }

    public synchronized Set<Position> getAttachedPositions() {
        return attachedBlocks;
    }

    public synchronized void rotate(Rotation rotation) {
        Set<Position> rotatedAttachments = new HashSet<>();
        for (Position p : attachedBlocks) {
            rotatedAttachments.add(rotation.rotate(p));
        }

        attachedBlocks.clear();
        attachedBlocks.addAll(rotatedAttachments);
    }


    public synchronized boolean isAttachedPercept(MapPercept mapPercept) {
        if (mapPercept == null)
            return false;

        Position relativePos = mapPercept.getLocation().subtract(getCurrentLocation());
        return mapPercept.hasBlock() && attachedBlocks.contains(relativePos);
    }

    public synchronized void detachBlock(Position position) {
        if (position == null)
            return;

        this.sharedAttachments.remove(position);
        attachedBlocks.remove(position);
    }

    public synchronized void taskSubmitted() {
        attachedBlocks.clear();
    }

    public synchronized AgentPerceptContainer getAgentPerceptContainer() {

        // Wait for percepts if they haven't been set yet.
        if (this.perceptContainer == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return perceptContainer;
    }


    public synchronized void updateMap() {
        agentMap.updateMap();
    }

    public synchronized SharedPerceptContainer getSharedPerceptContainer() {
        return getAgentPerceptContainer().getSharedPerceptContainer();
    }

    public synchronized void synchronizeMap() {
        agentAuthentication.pullMapPerceptsFromAgents();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentContainer)) return false;

        AgentContainer that = (AgentContainer) o;

        return Objects.equals(agentName, that.agentName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentName);
    }

    @Override
    public String toString() {
        return "Container of " + agentName;
    }

    public synchronized Set<Position> getPreviouslyRemovedAttachments() {
        return removedAttachments;
    }

    public synchronized Set<Position> getPreviouslyAddedAttachments() {
        return addedAttachments;
    }

    public synchronized Map<AgentContainer, Position> getRecentConnections() {
        return addedConnection;
    }
}
