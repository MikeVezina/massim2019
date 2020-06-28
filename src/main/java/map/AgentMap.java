package map;

import eis.agent.AgentContainer;
import eis.agent.AuthenticatedAgent;
import eis.percepts.terrain.ForbiddenCell;
import eis.percepts.terrain.FreeSpace;
import eis.percepts.terrain.Goal;
import eis.percepts.terrain.Terrain;
import eis.percepts.things.Entity;
import eis.percepts.things.Thing;
import utils.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AgentMap {
    private static Logger LOG = Logger.getLogger(AgentMap.class.getName());
    private Graph mapKnowledge;
    private AgentContainer agentContainer;
    private Map<Position, MapPercept> currentPerceptions;
    private List<Position> forbiddenLocations;
    private List<MapPercept> currentStepChunks;
    private AgentNavigation agentNavigation;


    public AgentMap(AgentContainer agentContainer) {
        this.agentContainer = agentContainer;
        this.forbiddenLocations = new ArrayList<>();
        this.currentPerceptions = new HashMap<>();
        this.currentStepChunks = new ArrayList<>();

        this.mapKnowledge = new Graph(agentContainer);
        this.agentNavigation = new AgentNavigation(agentContainer);
    }

    public AgentContainer getAgentContainer() {
        return agentContainer;
    }

    public String getAgentName() {
        return agentContainer.getAgentName();
    }

    public Position getCurrentAgentPosition() {
        return agentContainer.getCurrentLocation();
    }

    public synchronized List<MapPercept> getCurrentStepChunks() {
        return currentStepChunks;
    }

    public synchronized MapPercept getMapPercept(Position absolutePosition) {
        return getMapGraph().get(absolutePosition);
    }

    public synchronized AgentNavigation getAgentNavigation() {
        return this.agentNavigation;
    }


    public synchronized Map<Position, MapPercept> getCurrentPercepts() {
        return currentPerceptions;
    }

    public synchronized void updateMap() {
        // Clear list for new percepts.
        currentPerceptions.clear();
        currentStepChunks.clear();


        // TODO Remove this. This is to debug auth positions.
        for (AuthenticatedAgent authenticatedAgent : this.agentContainer.getAgentAuthentication().getAuthenticatedAgents()) {

        }


        int vision = agentContainer.getSharedPerceptContainer().getVision();
        long currentStep = agentContainer.getSharedPerceptContainer().getStep();
        Position currentAgentPosition = agentContainer.getCurrentLocation();

        List<Thing> thingPerceptions = agentContainer.getAgentPerceptContainer().getThingList();
        List<Terrain> terrainPerceptions = agentContainer.getAgentPerceptContainer().getTerrainList();

        // Create an area around the current position with the current percept visions.
        // All Spaces are automatically filled in with FreeSpace terrain.
        for (Position pos : new Utils.Area(currentAgentPosition, vision)) {
            currentPerceptions.put(pos, new MapPercept(pos, agentContainer, currentStep));
            Terrain defaultTerrain = new FreeSpace(pos.subtract(currentAgentPosition));

            // Handle forbidden locations differently since this is technically a "fake" terrain
            if (forbiddenLocations.contains(pos))
                defaultTerrain = new ForbiddenCell(pos);

            currentPerceptions.get(pos).setTerrain(defaultTerrain);
        }

        for (Thing thing : thingPerceptions) {
            Position absolutePos = currentAgentPosition.add(thing.getPosition());
            MapPercept mapPercept = currentPerceptions.get(absolutePos);

            if (mapPercept == null)
                LOG.warning("Null mapPercept. Possibly an invalid vision parameter: " + vision);
            else
                mapPercept.addThing(thing);
        }

        for (Terrain terrain : terrainPerceptions) {
            Position absolutePos = currentAgentPosition.add(terrain.getPosition());
            MapPercept mapPercept = currentPerceptions.get(absolutePos);

            if (mapPercept == null)
                LOG.warning("Null mapPercept. Possibly an invalid vision parameter: " + vision);
            else
                mapPercept.setTerrain(terrain);

        }

        updateMapChunk(currentPerceptions.values());
    }


    /**
     * Updates a chunk of the map.
     *
     * @param mapPerceptChunk The updated perceptions received by the agent.
     */
    public synchronized void updateMapChunk(Collection<MapPercept> mapPerceptChunk) {
        if (mapPerceptChunk == null || mapPerceptChunk.isEmpty())
            return;

        // Get all positions on the other agent map and add to our own knowledge
        List<MapPercept> updatedMapChunk = mapPerceptChunk.stream().filter(this::shouldUpdateMapPercept).collect(Collectors.toList());

        // Update our map knowledge
        mapKnowledge.prepareChunk(updatedMapChunk);

        // We want the agent to update their attachments at this point
        agentContainer.updateAttachments();

        // Update the graph connections
        mapKnowledge.updateChunkEdges(updatedMapChunk);

        // Add the chunk to the current step chunk
        currentStepChunks.addAll(updatedMapChunk);
    }

    /**
     * Updates a Single map location based on the given MapPercept
     *
     * @param updatePercept
     */
    private synchronized boolean shouldUpdateMapPercept(MapPercept updatePercept) {
        MapPercept currentPercept = mapKnowledge.get(updatePercept.getLocation());

        // We want to preserve Forbidden cells, since they are not perceived explicitly.
        if (updatePercept.getTerrain() instanceof ForbiddenCell)
            addForbiddenLocation(agentContainer.absoluteToRelativeLocation(updatePercept.getLocation()));

        // If we dont have a percept at the location, or if the existing information is older, set it.
        return currentPercept == null || currentPercept.getLastStepPerceived() < updatePercept.getLastStepPerceived();
    }

    /**
     * Get the MapPercept object in the relative direction of the agent.
     *
     * @param dir
     * @return
     */
    public MapPercept getRelativePerception(Direction dir) {
        if (dir == null || dir.equals(Direction.NONE))
            return null;
        Position pos = getCurrentAgentPosition().add(dir.getPosition());
        return mapKnowledge.get(pos);
    }


    public synchronized Graph getMapGraph() {
        return this.mapKnowledge;
    }


    public synchronized Map<Direction, MapPercept> getSurroundingPercepts(MapPercept percept) {
        if (percept == null)
            return null;

        Map<Direction, MapPercept> mapPercepts = new HashMap<>();
        for (Direction direction : Direction.validDirections()) {
            Position absolute = percept.getLocation().add(direction.getPosition());
            MapPercept dirPercept = getMapGraph().get(absolute);

            if (dirPercept != null)
                mapPercepts.put(direction, dirPercept);
        }

        return mapPercepts;
    }


    public synchronized MapPercept getSelfPercept() {
        return mapKnowledge.get(getCurrentAgentPosition());
    }

    public boolean doesBlockAgent(MapPercept percept) {
        return percept == null || (percept.isBlocking(getSelfPercept()));
    }

    public synchronized boolean isAgentBlocked(Direction direction) {
        if (direction == null)
            return false;

        Position dirResult = getCurrentAgentPosition().add(direction.getPosition());
        MapPercept dirPercept = mapKnowledge.get(dirResult);

        if (dirPercept == null)
            return false;

        return !getAgentContainer().getAttachedPositions().contains(direction.getPosition()) && dirPercept.isBlocking(getSelfPercept());
    }

    public synchronized void addForbiddenLocation(Position position) {
        Position absolute = agentContainer.relativeToAbsoluteLocation(position);

        if (forbiddenLocations.contains(absolute))
            return;

        this.forbiddenLocations.add(absolute);
    }

    private synchronized int getGoalClusterSize(MapPercept percept) {
        Set<MapPercept> percepts = new HashSet<>();
        addToGoalCluster(percept, percepts);
        return percepts.size();
    }

    private synchronized void addToGoalCluster(MapPercept percept, Set<MapPercept> goalCluster) {
        if (!(percept.getTerrain() instanceof Goal) || percept.isBlocking(getSelfPercept()) || goalCluster.contains(percept))
            return;

        goalCluster.add(percept);


        for (var entry : getSurroundingPercepts(percept).entrySet()) {
            Direction dir = entry.getKey();

            if(dir == Direction.NORTH)
                continue;

            MapPercept per = entry.getValue();
            addToGoalCluster(per, goalCluster);
        }

    }


    public List<MapPercept> getSortedGoalPercepts(Predicate<MapPercept> filter) {
        List<MapPercept> percepts = mapKnowledge.getCache().getCachedTerrain().stream()
                .filter(p -> p.getTerrain() instanceof Goal)
                .filter(filter)
                .sorted((g1, g2) -> {
                    int dist1 = getGoalClusterSize(g1);
                    int dist2 = getGoalClusterSize(g2);

                    return dist2 - dist1;
                })
                .collect(Collectors.toList());

        return percepts;
    }
}
