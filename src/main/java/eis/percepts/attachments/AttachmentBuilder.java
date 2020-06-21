package eis.percepts.attachments;

import eis.agent.AgentContainer;
import map.Direction;
import map.MapPercept;
import map.Position;
import eis.percepts.things.Thing;

import java.util.*;

/**
 * The responsibility of this class is to examine the current immediate perceptions of the agent
 * and determine whether which blocks are attached to which agents. There may be cases where an attachment
 * is perceived as connected to more than one agent (which may or may not be the correct case).
 */
public class AttachmentBuilder {
    private AgentContainer agentContainer;

    public AttachmentBuilder(AgentContainer agentContainer)
    {
        this.agentContainer = agentContainer;
    }

    private Set<Position> getAttachmentPerceptPositions()
    {
        var attachments = new HashSet<>(agentContainer.getAgentPerceptContainer().getRawAttachments());
        attachments.removeAll(agentContainer.getPreviouslyRemovedAttachments());
        attachments.addAll(agentContainer.getPreviouslyAddedAttachments());
        return attachments;
    }


    public Set<Position> getAttachments()
    {
        Set<Position> attachmentPositions = getAttachmentPerceptPositions();

        // Handle no attachment perceptions
        if(attachmentPositions.isEmpty())
            return Set.of();

        // Iterate through all of our surrounding percepts and try to determine which ones are attached to us
        Map<Direction, MapPercept> surroundingPercepts = agentContainer.getAgentMap().getSurroundingPercepts(agentContainer.getAgentMap().getSelfPercept());

        Map<Position, AttachedThing> allAttachedChains = new HashMap<>();

        for(var percept : surroundingPercepts.entrySet())
        {
            if(attachmentPositions.contains(percept.getKey().getPosition()))
            {
                allAttachedChains.putAll(createAttachmentChain(percept.getKey().getPosition(), percept.getValue()));
            }
        }
        return Set.copyOf(allAttachedChains.keySet());
    }

    private Map<Position, AttachedThing> createAttachmentChain(Position initialPerceptLocation, MapPercept initialPercept)
    {
        Map<Position, AttachedThing> attachedChain = new HashMap<>();
        recursiveCreateAttachmentChain(attachedChain, initialPerceptLocation, initialPercept);

        // We now want to iterate through all of the attached chain things to check if it is possible that some
        // blocks are connected to other entities. If so, it is possible that the whole chain belongs to the other entity.
        // In that case, we can only rely on previous knowledge of which blocks have been attached in the past.
        if(attachedChain.values().stream().anyMatch(a -> !a.getConnectedEntities().stream().allMatch(e -> getAttachmentPerceptPositions().contains(agentContainer.absoluteToRelativeLocation(e.getPosition())))))
            // Remove any entries that have not previously been attached to the agent.
            attachedChain.entrySet().removeIf(e -> !agentContainer.getAttachedPositions().contains(e.getKey()));

        return attachedChain;
    }

    private void recursiveCreateAttachmentChain(Map<Position, AttachedThing> currentChain, Position perceptLocation, MapPercept currentPercept)
    {
        // Ensure we do not get any cycles. If we already have the current perception in our chain, don't track it twice
        // Also, do not check the current perception if it is not tagged as an attachment
        if(currentChain.containsKey(perceptLocation) || !getAttachmentPerceptPositions().contains(perceptLocation))
            return;

        Thing attachedPercept = currentPercept.getAttachableThing();

        if(attachedPercept == null) {
            System.out.println("Failed to find an appropriate attachable thing type.");
        }

        Map<Direction, MapPercept> surroundingPercepts = agentContainer.getAgentMap().getSurroundingPercepts(currentPercept);

        AttachedThing attachedThing = new AttachedThing(perceptLocation, attachedPercept);

        // Insert the current percept attached thing object
        currentChain.put(perceptLocation, attachedThing);

        for(Map.Entry<Direction, MapPercept> perceptEntry : surroundingPercepts.entrySet())
        {
            Direction traversedDirection = perceptEntry.getKey();
            MapPercept traversedPercept = perceptEntry.getValue();
            Position nextPerceptLocation = perceptLocation.add(traversedDirection.getPosition());

            recursiveCreateAttachmentChain(currentChain,nextPerceptLocation,traversedPercept);

            // Add any entities that may be connected.
            // As long as an entity is beside a connected block, it is possible for them to be connected.
            // The server does not provide us with any information about which blocks are attached to which agent, so we
            // have to do some further processing to see if it is our attached block or a block attached to another agent.
            // Also, make sure the percept is not the originating agent (aka relative perception position (0,0))
            if(traversedPercept.hasEntity() && !nextPerceptLocation.equals(Position.ZERO))
                attachedThing.addConnectedEntity(traversedPercept.getEntity());
        }
    }


//    /**
//     * Create a chain of attachments from the current location perception (aka MapPercept). Since this is a
//     * recursive call, we do not want to traverse a direction that has already been traversed.
//     *
//     * @param traversedLocation The direction that was taken from the previous location to get to this location
//     *                          (should not be null since we should start traversal from an entity attached to a thing).
//     *
//     * @param currentPercept The current MapPercept containing the "thing" that is attached.
//     *
//     * @return The AttachedThing object (which includes a chain of other attached things)
//     */
//    private AttachedThing createAttachmentChainOld(Position baseLocation, Position previousLocation, MapPercept currentPercept) {
//        if(previousLocation == null)
//            throw new InvalidParameterException("traversedLocation should not be null or NONE.");
//
//        // Get the direction of the previous attached entity/thing (it will be the opposite of the traversed location)
//        Direction previousDirection = Direction.GetDirection(previousLocation.subtract(currentPercept.getLocation()));
//        Position baseOffset = currentPercept.getLocation().subtract(baseLocation);
//
//        // Obtain the perceived attachment locations
//        List<Position> attachmentPositions = getAttachmentPerceptPositions();
//
//        Map<Direction, MapPercept> surroundingPercepts = agentContainer.getAgentMap().getSurroundingPercepts(currentPercept);
//
//        Thing attachedPercept = currentPercept.getAttachableThing();
//
//        if(attachedPercept == null)
//            throw new RuntimeException("Failed to find an appropriate attachable thing type.");
//
//        AttachedThing attachedThing = new AttachedThing(baseOffset, attachedPercept);
//
//        for(Map.Entry<Direction, MapPercept> perceptEntry : surroundingPercepts.entrySet())
//        {
//            Direction traversedDirection = perceptEntry.getKey();
//            MapPercept traversedPercept = perceptEntry.getValue();
//
//            // Don't look at the previous direction
//            if(traversedDirection.equals(previousDirection))
//                continue;
//
//            // We want to look for attached blocks or entities
//            if(attachmentPositions.contains(baseOffset.add(traversedDirection.getPosition())))
//            {
//                AttachedThing furtherChain = createAttachmentChainOld(baseLocation, currentPercept.getLocation(), traversedPercept);
//                attachedThing.addAttachment(furtherChain);
//            }
//
//            // Add any entities that may be connected.
//            // As long as an entity is beside a connected block, it is possible for them to be connected.
//            // The server does not provide us with any information about which blocks are attached to which agent
//            if(traversedPercept.hasEntity())
//                attachedThing.addConnectedEntity(traversedPercept.getEntity());
//        }
//
//        return attachedThing;
//    }

    /**
     * This method builds a list of
     *
     * @param agentContainer The agent container containing all parsed percept information.
     * @return A list of things that are attached to the agent container
     */
    public List<AttachedThing> parseAttachments(AgentContainer agentContainer) {

        // Get a list of perceived attachments (these are not necessarily attached to us).
        List<Position> attachmentLocations = agentContainer.getAgentPerceptContainer().getRawAttachments();

        // Get our current vision percepts
        Map<Position, MapPercept> currentPercepts = agentContainer.getAgentMap().getCurrentPercepts();

        if (currentPercepts.isEmpty())
            throw new RuntimeException("The current perceptions should not be empty");


        // We want to iterate through all perceived attachments and check if they are our own.
        for (Position attached : attachmentLocations) {
            Position absolutePosition = agentContainer.relativeToAbsoluteLocation(attached);
            MapPercept percept = currentPercepts.get(absolutePosition);

            if (percept == null)
                throw new RuntimeException("The attached perception should not be null");


        }

        return new ArrayList<>();
    }

}
