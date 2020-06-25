package utils;

import eis.percepts.requirements.AttachableRequirement;
import eis.percepts.requirements.Requirement;
import map.Direction;
import map.Position;

import java.util.*;

import static map.Direction.*;


public final class RequirementPlanner {

    public static Deque<Requirement> SortRequirements(List<Requirement> originalRequirementList) {

        if (originalRequirementList == null)
            throw new NullPointerException("Requirements List can not be null.");

        if (originalRequirementList.isEmpty())
            throw new NullPointerException("Requirements List can not be empty.");


        // Copy the requirements list for processing
        List<Requirement> requirementList = new ArrayList<>(originalRequirementList);

        // Sort by distance. This greatly reduces the amount of iterations needed to create the sequence of directions.
        // Also ensures first requirement in list is 0,1
        requirementList.sort(RequirementPlanner::compare);

        // The first requirement needs to be at (0, 1)
        if (!requirementList.get(0).getPosition().equals(SOUTH)) {
            throw new RuntimeException("The first requirement should always be south.");
        }

        // If there is only one requirement, there is no need to sort them. Add to list and return.
        if (requirementList.size() == 1)
            return new LinkedList<>(requirementList);




        Map<Position, Requirement> requirementPositionLookup = new HashMap<>();
        LinkedHashMap<Requirement, Set<Requirement>> allAdjacentRequirements = new LinkedHashMap<>();
        Map<Requirement, Set<Requirement>> incomingEdgeMap = new HashMap<>();
        Map<Requirement, Set<Requirement>> outgoingEdgeMap = new HashMap<>();

        // Put empty requirement sets for all requirements
        for (Requirement requirement : requirementList) {
            incomingEdgeMap.put(requirement, new HashSet<>());
            outgoingEdgeMap.put(requirement, new HashSet<>());
            allAdjacentRequirements.put(requirement, new HashSet<>());
            requirementPositionLookup.put(requirement.getPosition(), requirement);
        }

        // Put all adjacent requirements in the map
        for (Requirement requirement : allAdjacentRequirements.keySet()) {
            Set<Requirement> adjacentRequirements = new HashSet<>();

            for (Direction direction : Direction.validDirections()) {
                var translatedPosition = requirement.getPosition().add(direction.getPosition());

                if (!requirementPositionLookup.containsKey(translatedPosition))
                    continue;

                var adjacentRequirement = requirementPositionLookup.get(translatedPosition);
                adjacentRequirements.add(adjacentRequirement);
            }

            allAdjacentRequirements.put(requirement, adjacentRequirements);
        }

        for (var adjacentRequirementEntry : allAdjacentRequirements.entrySet()) {
            Requirement requirement = adjacentRequirementEntry.getKey();
            Set<Requirement> adjacentRequirements = adjacentRequirementEntry.getValue();

            var outgoingEdgeSet = outgoingEdgeMap.get(requirement);
            var incomingEdgeSet = incomingEdgeMap.get(requirement);

            for(Requirement adjacent : adjacentRequirements)
            {
                // Don't add the adj. requirement if it's already an incoming edge
                if(incomingEdgeSet.contains(adjacent))
                    continue;

                // Add to outgoing edges
                outgoingEdgeSet.add(adjacent);

                // Add current requirement to incoming edges of adj.
                var adjacentIncoming = incomingEdgeMap.get(adjacent);
                adjacentIncoming.add(requirement);
            }

        }

        return topologicalSort(incomingEdgeMap, outgoingEdgeMap);
    }

    private static Deque<Requirement> topologicalSort(Map<Requirement, Set<Requirement>> incomingEdges, Map<Requirement, Set<Requirement>> outgoingEdges)
    {
        Deque<Requirement> topologicalResult = new LinkedList<>();
        Set<Requirement> visitedNodes = new HashSet<>();
        Queue<Requirement> topologicalQueue = new LinkedList<>();

        // Add all empty incoming edge nodes to queue
        for(var incomingEntry : incomingEdges.entrySet())
        {
            if(incomingEntry.getValue().isEmpty())
                topologicalQueue.add(incomingEntry.getKey());
        }


        while (!topologicalQueue.isEmpty())
        {
            var nextResult = topologicalQueue.poll();

            if(visitedNodes.contains(nextResult))
                continue;

            // Visit node and add to result
            visitedNodes.add(nextResult);
            topologicalResult.add(nextResult);

            // Add all outgoing edges to queue
            topologicalQueue.addAll(outgoingEdges.get(nextResult));
        }

        return topologicalResult;
    }

    public static int compare(Requirement o1, Requirement o2) {
        return Double.compare(o1.getPosition().getDistance(), o2.getPosition().getDistance());
    }
}
