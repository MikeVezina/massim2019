package eis.percepts.attachments;

import eis.agent.AgentContainer;
import eis.percepts.things.Entity;
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

    public AttachmentBuilder(AgentContainer agentContainer) {
        this.agentContainer = agentContainer;
    }

    private Set<Position> getAttachmentPerceptPositions() {
        var attachments = new HashSet<>(agentContainer.getAgentPerceptContainer().getRawAttachments());
//        attachments.removeAll(agentContainer.getPreviouslyRemovedAttachments());
//        attachments.addAll(agentContainer.getPreviouslyAddedAttachments());
        return attachments;
    }


    public Set<Position> getAttachments() {
        Set<Position> attachmentPositions = getAttachmentPerceptPositions();

        // Handle no attachment perceptions
        if (attachmentPositions.isEmpty())
            return Set.of();

        // Iterate through all of our surrounding percepts and try to determine which ones are attached to us
        Map<Direction, MapPercept> surroundingPercepts = agentContainer.getAgentMap().getSurroundingPercepts(agentContainer.getAgentMap().getSelfPercept());

        Map<Position, AttachedThing> allAttachedChains = new HashMap<>();

        for (var percept : surroundingPercepts.entrySet()) {
            if (attachmentPositions.contains(percept.getKey().getPosition())) {
                allAttachedChains.putAll(createAttachmentChain(percept.getKey().getPosition(), percept.getValue()));
            }
        }
        return Set.copyOf(allAttachedChains.keySet());
    }

    private Map<Position, AttachedThing> createAttachmentChain(Position initialPerceptLocation, MapPercept initialPercept) {
        Map<Position, AttachedThing> attachedChain = new HashMap<>();
        recursiveCreateAttachmentChain(attachedChain, initialPerceptLocation, initialPercept);

        // Checks if any blocks are connected to an entity
        boolean hasConnectedEntity = attachedChain.values().stream()
                .anyMatch(a -> !a.getConnectedEntities().stream()
                        .allMatch(e -> getAttachmentPerceptPositions().contains(agentContainer.absoluteToRelativeLocation(e.getPosition()))));

        // Blocks are not connected to any entities but are attached (they must be ours)
        if (!hasConnectedEntity)
            return attachedChain;

        // Remove any entries that have not previously been attached to the agent by a connection with another agent.
        if (agentContainer.getRecentConnections().isEmpty())
        {
            attachedChain.entrySet().removeIf(a -> this.shouldRemoveFromChain(a.getKey(), a.getValue()));
            return attachedChain;
        }


        // Get all connected entities and check if we have connected with any of them
        // If so, keep the chain
        boolean hasValidConnectedEntity = attachedChain.values().stream()
                .anyMatch(this::isConnected);

        if(hasValidConnectedEntity)
            return attachedChain;


        // Remaining blocks are not connected
        attachedChain.entrySet().removeIf(a -> this.shouldRemoveFromChain(a.getKey(), a.getValue()));
        return attachedChain;


        // We will not keep track of any attachments that are near another agent NOT connected to us
//        var attIterator = attachedChain.entrySet().iterator();
//        while (attIterator.hasNext()) {
//            var attachmentEntry = attIterator.next();
//
//            Position attachPosition = attachmentEntry.getKey();
//            AttachedThing attachThing = attachmentEntry.getValue();
//
//
//            // If the attachment was previously attached (last step) just skip to the next attachment
//            if (agentContainer.getAttachedPositions().contains(attachPosition) || isConnected(attachThing))
//                continue;
//
//            // Remove the attachment if we did not recently connect and the attachment is near another agent (Or is the other agent).
//            attIterator.remove();
//        }


        // We now want to iterate through all of the attached chain things to check if it is possible that some
//        // blocks are connected to other entities. If so, it is possible that the whole chain belongs to the other entity.
//        // In that case, we can only rely on previous knowledge of which blocks have been attached in the past.
//        if(attachedChain.values().stream()
//                .anyMatch(a -> !a.getConnectedEntities().stream()
//                        .allMatch(e -> getAttachmentPerceptPositions().contains(agentContainer.absoluteToRelativeLocation(e.getPosition())))))
//            // Remove any entries that have not previously been attached to the agent.
//            attachedChain.entrySet().removeIf(e -> !agentContainer.getAttachedPositions().contains(e.getKey()));


    }

    // Remove the block if we have just recently removed it and if we havent just attached it, or if it wasn't attached to us previously
    private boolean shouldRemoveFromChain(Position position, AttachedThing thing) {
        return (agentContainer.getPreviouslyRemovedAttachments().contains(position) && !agentContainer.getPreviouslyAddedAttachments().contains(position)) || !agentContainer.getAttachedPositions().contains(position);
    }

    /**
     * Check if an attached entity or block are connected via a connected entity
     *
     * @param thing
     * @return True if the entity was connected in the last step, or if the thing is connected via an entity that was recently connected
     */
    private boolean isConnected(AttachedThing thing) {

        // Do not add entities
        if (thing.getThing() instanceof Entity)
            // Check if the entity was recently connected
            return false; // return isEntityConnected((Entity) thing.getThing());

        // Check connected entities
        return thing.getConnectedEntities().isEmpty() || thing.getConnectedEntities().stream().anyMatch(this::isEntityConnected);
    }

    private boolean isEntityConnected(Entity thing) {

        if (agentContainer.getRecentConnections().isEmpty())
            return false;

        // Check if absolute
        boolean isThingAbsolute = isThingAbsolute(thing);

        // Get the absolute position of the entity perception
        var absPosition = isThingAbsolute ? thing.getPosition() : agentContainer.relativeToAbsoluteLocation(thing.getPosition());


        // att pos = 1, -2
        // Get the connected containers
        for (var container : agentContainer.getRecentConnections().keySet()) {
            var teamPosition = agentContainer.getAgentAuthentication().getAuthenticatedTeammatePositions().get(container);

            if (absPosition.equals(teamPosition))
                return true;
        }

        return false;
    }

    private boolean isThingAbsolute(Entity thing) {
        var percepts = agentContainer.getAgentMap().getCurrentPercepts();
        return percepts.containsKey(thing.getPosition()) && percepts.get(thing.getPosition()).getThingList().contains(thing);
    }

    private void recursiveCreateAttachmentChain(Map<Position, AttachedThing> currentChain, Position perceptLocation, MapPercept currentPercept) {
        // Ensure we do not get any cycles. If we already have the current perception in our chain, don't track it twice
        // Also, do not check the current perception if it is not tagged as an attachment
        if (currentChain.containsKey(perceptLocation) || !getAttachmentPerceptPositions().contains(perceptLocation))
            return;

        Thing attachedPercept = currentPercept.getAttachableThing();

        if (attachedPercept == null) {
            System.out.println("Failed to find an appropriate attachable thing type.");
        }

        Map<Direction, MapPercept> surroundingPercepts = agentContainer.getAgentMap().getSurroundingPercepts(currentPercept);

        AttachedThing attachedThing = new AttachedThing(perceptLocation, attachedPercept);

        // Insert the current percept attached thing object
        currentChain.put(perceptLocation, attachedThing);

        for (Map.Entry<Direction, MapPercept> perceptEntry : surroundingPercepts.entrySet()) {
            Direction traversedDirection = perceptEntry.getKey();
            MapPercept traversedPercept = perceptEntry.getValue();
            Position nextPerceptLocation = perceptLocation.add(traversedDirection.getPosition());

            recursiveCreateAttachmentChain(currentChain, nextPerceptLocation, traversedPercept);

            // Add any entities that may be connected.
            // As long as an entity is beside a connected block, it is possible for them to be connected.
            // The server does not provide us with any information about which blocks are attached to which agent, so we
            // have to do some further processing to see if it is our attached block or a block attached to another agent.
            // Also, make sure the percept is not the originating agent (aka relative perception position (0,0))
            if (traversedPercept.hasEntity() && !nextPerceptLocation.equals(Position.ZERO))
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
