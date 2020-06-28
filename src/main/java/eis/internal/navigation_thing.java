package eis.internal;

import eis.EISAdapter;
import map.AgentMap;
import jason.NoValueException;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import map.Direction;
import map.MapPercept;
import map.Position;

import java.util.List;
import java.util.Map;

public class navigation_thing extends DefaultInternalAction {


    private static final long serialVersionUID = -6214881485708125130L;
    private static final Atom NORTH = new Atom("n");
    private static final Atom SOUTH = new Atom("s");
    private static final Atom WEST = new Atom("w");
    private static final Atom EAST = new Atom("e");


    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {

        AgentMap agentMap = EISAdapter.getSingleton().getAgentMap(ts.getUserAgArch().getAgName());


        Literal type = (Literal) args[0];
        Literal details = (Literal) args[1];

        Position thingDestination = findThing(agentMap, type.getFunctor(), details.getFunctor());


        if (thingDestination == null)
            return false;

        Structure location = ASSyntax.createStructure("location", ASSyntax.createNumber(thingDestination.getX()), ASSyntax.createNumber(thingDestination.getY()));

        // Unify
        return un.unifies(location, args[2]);
    }

    private Position findThing(AgentMap map, String type, String details) {
        // It would be good to sort these by how recent the perceptions are, and the distance.
        MapPercept percept = map.getMapGraph().getCache().getCachedThingList().stream()
                .filter(p -> p.hasThing(type, details))
                .min((p1, p2) -> {
                    if (map.doesBlockAgent(p1) && !map.doesBlockAgent(p2))
                        return 1;

                    else if (!map.doesBlockAgent(p1) && map.doesBlockAgent(p2))
                        return -1;

                    // Compare the distance if both are the same
                    double dist1 = map.getAgentContainer().getCurrentLocation().subtract(p1.getLocation()).getDistance();
                    double dist2 = map.getAgentContainer().getCurrentLocation().subtract(p2.getLocation()).getDistance();

                    int compared = Double.compare(dist1, dist2);
                    if(compared == 0)
                        return (int) (p1.getLastStepPerceived() - p2.getLastStepPerceived());
                    else
                        return compared;

                }).orElse(null);

        if (percept == null)
            return null;

        List<Position> shortestPath = map.getAgentNavigation().getNavigationPath(percept.getLocation());

        if (shortestPath != null) {
            // Remove the THING location from the navigation path
            shortestPath.removeIf(p -> p.equals(percept.getLocation()));

            // Return the destination position
            if (shortestPath.isEmpty()) {
                // We are right next to the percept. No need to navigate
                System.out.println("We are next to the percept. Type: " + type + ". Details: " + details);
                return null;
            }

            return shortestPath.get(shortestPath.size() - 1);
        }

        // If the agent is blocked by the percept, no navigation path will be found.
        // This attempts to get a navigation path to the surrounding percepts
        for (Map.Entry<Direction, MapPercept> surroundingPercepts : map.getSurroundingPercepts(percept).entrySet()) {
            shortestPath = map.getAgentNavigation().getNavigationPath(surroundingPercepts.getValue().getLocation());

            // If we have a path to the percept, return the position
            if (shortestPath != null)
                return surroundingPercepts.getValue().getLocation();
        }

        // No path could be found
        return null;
    }

}
