package eis.percepts;

import eis.iilang.Percept;
import eis.percepts.handlers.AgentPerceptManager;
import eis.percepts.handlers.PerceptListener;
import eis.percepts.terrain.ForbiddenCell;
import eis.percepts.terrain.FreeSpace;
import eis.percepts.terrain.Obstacle;
import eis.percepts.terrain.Terrain;
import eis.percepts.things.Thing;
import utils.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public class AgentMap implements PerceptListener {
    private static Logger LOG = Logger.getLogger(AgentMap.class.getName());
    private static int vision = -1;
    private Graph mapKnowledge;
    private Map<String, AgentMap> knownAgentMaps;
    private Map<String, Position> translationPositions;
    private AgentContainer agentContainer;

    public AgentMap(AgentContainer agentContainer) {
        this.agentContainer = agentContainer;

        this.mapKnowledge = new Graph(this);
        this.knownAgentMaps = new HashMap<>();
        this.translationPositions = new HashMap<>();
    }

    public static void setVision(int vision) {
        AgentMap.vision = vision;
    }

    public static int getVision() {
        if (vision == -1)
            throw new RuntimeException("Vision has not been set.");

        return vision;
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



    public void agentAuthenticated(String agentName, Position translation, AgentMap agentMap) {
        knownAgentMaps.put(agentName, agentMap);
        translationPositions.put(agentName, translation);

        agentMap.knownAgentMaps.put(this.getAgentName(), this);
        agentMap.translationPositions.put(this.getAgentName(), translation.negate());

        for (MapPercept percept : getMapKnowledge().values()) {
            MapPercept translatedPercept = percept.copyToAgent(translation);
            agentMap.agentFinalizedPercept(this.getAgentName(), translatedPercept);
        }
    }

    private void updateMapLocation(MapPercept updatePercept) {
        MapPercept currentPercept = mapKnowledge.getOrDefault(updatePercept.getLocation(), null);

        // If we dont have a percept at the location, set it.
        if (currentPercept == null) {
            mapKnowledge.put(updatePercept.getLocation(), updatePercept);
            return;
        }

        // If we do have a perception at the location, but ours is older, then update/overwrite it.
        if (currentPercept.getLastStepPerceived() < updatePercept.getLastStepPerceived())
            mapKnowledge.put(updatePercept.getLocation(), updatePercept);
    }

    private void agentFinalizedPercept(String agent, MapPercept updatedPercept) {
        //updateMapLocation(updatedPercept);
    }

    public List<MapPercept> getRelativePerceptions(int range) {
        if (range <= 0)
            return new ArrayList<>();

        List<MapPercept> perceptList = new ArrayList<>();

        for (Position p : new Utils.Area(getCurrentAgentPosition(), range)) {
            MapPercept relativePercept = new MapPercept(mapKnowledge.get(p));
            relativePercept.setLocation(p.subtract(getCurrentAgentPosition()));

            perceptList.add(mapKnowledge.get(p));
        }

        return perceptList;
    }

    private Map<Position, MapPercept> getMapKnowledge() {
        return Collections.unmodifiableMap(mapKnowledge);
    }

    public Graph getMapGraph() {
        return this.mapKnowledge;
    }

    /**
     * @return
     */
    public List<Position> getNavigationPath(Position absoluteDestination) {
        return mapKnowledge.getShortestPath(getCurrentAgentPosition(), absoluteDestination);
    }

    public Position relativeToAbsoluteLocation(Position relative) {
        return getCurrentAgentPosition().add(relative);
    }

    public Position absoluteToRelativeLocation(Position absolute) {
        return absolute.subtract(getCurrentAgentPosition());
    }

    private MapPercept getTranslatedPercept(String agent, MapPercept percept) {
        Position translation = translationPositions.get(agent);
        return percept.copyToAgent(translation);
    }

    public synchronized void finalizeStep() {
//        currentStepKnowledge.entrySet().parallelStream().forEach(e -> {
//                    Position currentPosition = e.getKey();
//                    MapPercept currentPercept = e.getValue();
//
//                    // Check to see if the cell is forbidden
//                    MapPercept lastStepPercept = mapKnowledge.get(e.getKey());
//
//                    if (lastStepPercept != null && lastStepPercept.getTerrain() != null && lastStepPercept.getTerrain() instanceof ForbiddenCell) {
//                        // Do nothing.
//                    } else {
//                        updateMapLocation(e.getValue());
//                    }
//                }
//        );
//
//        //currentStepKnowledge.values().parallelStream().map(p -> p.copyToAgent())
//
//        for (MapPercept percept : currentStepKnowledge.values()) {
//            for (AgentMap map : knownAgentMaps.values()) {
//                if (percept.getAgentSource().equals(map.agent))
//                    continue;
//                map.agentFinalizedPercept(this.agent, getTranslatedPercept(map.agent, percept));
//            }
//        }
//
//        mapKnowledge.redraw();
    }

    public MapPercept getSelfPercept() {
        return mapKnowledge.get(getCurrentAgentPosition());
    }

    public boolean doesBlockAgent(MapPercept percept) {
        return percept == null || percept.isBlocking(getSelfPercept());
    }


    public boolean isAgentBlocked(Direction direction) {
        if (direction == null)
            return false;

        Position dirResult = getCurrentAgentPosition().add(direction.getPosition());
        MapPercept dirPercept = mapKnowledge.get(dirResult);

        return dirPercept == null || dirPercept.isBlocking(getSelfPercept());

    }

    public void addForbidden(Position dirPos) {
        Position absolute = getCurrentAgentPosition().add(dirPos);

        MapPercept percept = mapKnowledge.getOrDefault(absolute, new MapPercept(absolute, this.getAgentName(), agentContainer.getCurrentStep()));
        percept.setTerrain(new ForbiddenCell(absolute));
        mapKnowledge.put(absolute, percept);

        System.out.println(dirPos);
    }

    public boolean containsEdge(Direction edgeDirection) {
        if (vision == -1)
            return false;

        int edgeScalar = AgentMap.getVision() + 1;
        Position absolute = getCurrentAgentPosition().add(edgeDirection.multiply(edgeScalar));
        return this.getMapGraph().containsKey(absolute);
    }


    @Override
    public void perceptsProcessed(AgentPerceptManager perceptManager) {
        String agentName = perceptManager.getAgentContainer().getAgentName();
        long currentStep = perceptManager.getAgentContainer().getCurrentStep();

        List<Thing> thingPerceptions = perceptManager.getThingPerceptHandler().getPerceivedThings();
        List<Terrain> terrainPerceptions = perceptManager.getTerrainPerceptHandler().getPerceivedTerrain();

        Map<Position, MapPercept> perceptMap = new HashMap<>();

        for (Position pos : new Utils.Area(getCurrentAgentPosition(), getVision())) {
            perceptMap.put(pos, new MapPercept(pos, agentName, currentStep));
            perceptMap.get(pos).setTerrain(new FreeSpace(pos.subtract(getCurrentAgentPosition())));
        }

        for(Thing thing : thingPerceptions)
        {
            Position absolutePos = getCurrentAgentPosition().add(thing.getPosition());
            MapPercept mapPercept = perceptMap.get(absolutePos);

            if (mapPercept == null) {
                LOG.info("Null: " + mapPercept);
            }

            mapPercept.setThing(thing);
        }

        for(Terrain terrain : terrainPerceptions)
        {
            Position absolutePos = getCurrentAgentPosition().add(terrain.getPosition());
            MapPercept mapPercept = perceptMap.get(absolutePos);

            if (mapPercept == null) {
                LOG.info("Null: " + mapPercept);
            }
            mapPercept.setTerrain(terrain);
        }

        perceptMap.forEach((key, value) -> {
            // Check to see if the cell is forbidden
            MapPercept lastStepPercept = mapKnowledge.get(key);
            List<Terrain> t = terrainPerceptions;

            if(lastStepPercept != null && lastStepPercept.getTerrain() != null && value.getTerrain() == null)
            {
                //
                System.out.println(t);
            }

            if (lastStepPercept != null && lastStepPercept.getTerrain() != null && lastStepPercept.getTerrain() instanceof ForbiddenCell) {
                // Do nothing.
            } else {
                updateMapLocation(value);
            }
        });

       mapKnowledge.redraw();
    }
}
