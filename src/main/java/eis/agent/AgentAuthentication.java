package eis.agent;

import eis.percepts.things.Entity;
import eis.watcher.SynchronizedPerceptWatcher;
import messages.Message;
import map.MapPercept;
import map.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AgentAuthentication {
    private static final Logger LOG = Logger.getLogger(AgentAuthentication.class.getName());
    private ConcurrentMap<String, AuthenticatedAgent> authenticatedAgentMap;
    private AgentContainer selfAgentContainer;

    public AgentAuthentication(AgentContainer selfAgentContainer) {
        this.selfAgentContainer = selfAgentContainer;
        this.authenticatedAgentMap = new ConcurrentHashMap<>();
    }

    public synchronized boolean canAuthenticate(AgentContainer otherAgentContainer) {
        return !selfAgentContainer.getAgentName().equals(otherAgentContainer.getAgentName()) && !authenticatedAgentMap.containsKey(otherAgentContainer.getAgentName());
    }

    public synchronized boolean canAuthenticate(AuthenticatedAgent otherAgentContainer) {
        return canAuthenticate(otherAgentContainer.getAgentContainer());
    }

    public synchronized void authenticateAgent(AgentContainer otherAgentContainer, Position translation) {
        if (!canAuthenticate(otherAgentContainer)) {
            System.out.println("Agent can not authenticate self or previously authenticated agent. Agent Name: " + otherAgentContainer.getAgentName());
            return;
        }

        // Debugging stuff
        List<Position> translations = new ArrayList<>();
        for (AuthenticatedAgent authenticatedAgent : authenticatedAgentMap.values()) {
            if (translations.contains(authenticatedAgent.getTranslationValue()) || authenticatedAgent.getTranslationValue().equals(translation)) {
//                throw new NullPointerException("Failed to authenticate " + otherAgentContainer);
            }
            translations.add(authenticatedAgent.getTranslationValue());

        }
        var relPositions = selfAgentContainer.getDebuggingLocations();
        if (relPositions != null) {
            relPositions.forEach((agent, thing) -> {
                if (thing instanceof Entity) {
                    if (otherAgentContainer.getAgentName().equals(agent.getAgentName()) && !translation.equals(thing.getPosition().negate())) {
                          System.out.println("Invalid auth. translation value!");
                    }
                }
            });
        }
        String agentName = otherAgentContainer.getAgentName();
        authenticatedAgentMap.put(agentName, new AuthenticatedAgent(otherAgentContainer, translation));

        // Merge the other agent's complete map (this actually also syncs any further agents that the otherAgent has synced with in the past)
        mergeCompleteMap(otherAgentContainer);

        // Add any agents that the otherAgent has synced with
        otherAgentContainer.getAgentAuthentication().getAuthenticatedAgents()
                .stream()
                .filter(this::canAuthenticate)
                .forEach(authAgent -> {
                    // Get and calculate the agent translation value
                    Position calculatedTranslation = authAgent.getTranslationValue().add(translation);
                    authenticatedAgentMap.put(authAgent.getAgentContainer().getAgentName(), new AuthenticatedAgent(authAgent.getAgentContainer(), calculatedTranslation));

                    // Add our translation value if the other agent doesn't have it
                    if (authAgent.getAgentContainer().getAgentAuthentication().canAuthenticate(selfAgentContainer))
                        authAgent.getAgentContainer().getAgentAuthentication().authenticatedAgentMap.put(selfAgentContainer.getAgentName(), new AuthenticatedAgent(selfAgentContainer, calculatedTranslation.negate()));

                });

    }

    public synchronized Position translateToAgent(AgentContainer agentContainer, Position position) {
        AuthenticatedAgent authenticatedAgent = authenticatedAgentMap.get(agentContainer.getAgentName());

        if (authenticatedAgent == null)
            return position;

        return position.subtract(authenticatedAgent.getTranslationValue());
    }

    public synchronized Position translateFromAgent(AgentContainer agentContainer, Position position) {
        AuthenticatedAgent authenticatedAgent = authenticatedAgentMap.get(agentContainer.getAgentName());

        if (authenticatedAgent == null)
            return position;

        return authenticatedAgent.getTranslationValue().subtract(position);
    }

    /**
     * Merges the full map from the other agent container. This is typically done
     * when two agents initially authenticate each other.
     *
     * @param otherAgentContainer The other agent container to merge from.
     */
    public synchronized void mergeCompleteMap(AgentContainer otherAgentContainer) {
        long startTime = System.nanoTime();
        mergeMapPercepts(otherAgentContainer.getAgentName(), otherAgentContainer.getAgentMap().getMapGraph().getCache());
        long deltaTime = (System.nanoTime() - startTime) / 1000000;

        if (deltaTime > 200) {
            LOG.warning("Agent " + selfAgentContainer.getAgentName() + " took " + deltaTime + " to perform a full merge with agent " + otherAgentContainer.getAgentName());
        }
    }

    /**
     * @return the absolute locations of all authenticated agents.
     */
    public synchronized Map<AgentContainer, Position> getAuthenticatedTeammatePositions() {
        Map<AgentContainer, Position> teammatePositions = new HashMap<>();

        for (var authenticatedAgent : getAuthenticatedAgentPositions().entrySet()) {
            teammatePositions.put(authenticatedAgent.getKey().getAgentContainer(), authenticatedAgent.getValue());
        }

        return teammatePositions;
    }

    public synchronized Map<AuthenticatedAgent, Position> getAuthenticatedAgentPositions() {
        Map<AuthenticatedAgent, Position> teammatePositions = new HashMap<>();

        for (AuthenticatedAgent authenticatedAgent : getAuthenticatedAgents()) {
            Position otherAgentLocation = authenticatedAgent.getAgentContainer().getCurrentLocation();

            Position translationValue = authenticatedAgent.getTranslationValue();
            teammatePositions.put(authenticatedAgent, otherAgentLocation.add(translationValue));
        }

        return teammatePositions;
    }


    /**
     * Iterates through all agent maps and pulls the current percepts.
     */
    public synchronized void pullMapPerceptsFromAgents() {
        authenticatedAgentMap.values().forEach(authAgent -> mergeMapPercepts(authAgent.getAgentContainer().getAgentName(), authAgent.getAgentContainer().getAgentMap().getCurrentPercepts()));

        Map<AgentContainer, Position> agentPositions = getAuthenticatedTeammatePositions();
        for (Map.Entry<AgentContainer, Position> authenticatedAgent : agentPositions.entrySet()) {
            if (agentPositions.values().stream().filter(p -> p.equals(authenticatedAgent.getValue())).count() > 1)
                throw new RuntimeException("Two Agents are at the same location.");

        }
        Message.createAndSendAuthenticatedMessage(selfAgentContainer.getMqSender(), getAuthenticatedAgents());
    }

    /**
     * Merges the provided percepts perceived by all other agents.
     *
     * @param agentName     The name of the agent we are syncing our map with
     * @param mapPerceptMap The updated perceptions received by the agent.
     */
    public synchronized void mergeMapPercepts(String agentName, Map<Position, MapPercept> mapPerceptMap) {
        if (agentName == null || mapPerceptMap == null || mapPerceptMap.isEmpty())
            return;

        Position translation = authenticatedAgentMap.get(agentName).getTranslationValue();

        if (translation == null)
            return;

        // Get all positions on the other agent map and add to our own knowledge
        List<MapPercept> perceptChunkUpdate = mapPerceptMap.values().parallelStream().map(percept -> percept.copyToAgent(translation.negate())).collect(Collectors.toList());
        selfAgentContainer.getAgentMap().updateMapChunk(perceptChunkUpdate);
    }

    public synchronized List<AuthenticatedAgent> getAuthenticatedAgents() {
        return new ArrayList<>(authenticatedAgentMap.values());
    }

    public AgentContainer findAgentByRelativePosition(int xAgent, int yAgent) {
        Position absolute = this.selfAgentContainer.relativeToAbsoluteLocation(new Position(xAgent, yAgent));

        for (var agentEntry : this.getAuthenticatedTeammatePositions().entrySet()) {
            if (agentEntry.getValue().equals(absolute))
                return agentEntry.getKey();
        }
        return null;
    }
}
