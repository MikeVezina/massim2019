package eis;

import eis.watcher.SynchronizedPerceptWatcher;
import jason.runtime.RuntimeServices;
import map.AgentMap;
import map.MapPercept;
import eis.agent.*;
import eis.percepts.things.Block;
import jason.NoValueException;
import jason.asSyntax.*;
import eis.exceptions.*;
import eis.iilang.*;
import jason.environment.Environment;
import massim.eismassim.EnvironmentInterface;
import map.Direction;
import utils.LiteralUtils;
import map.Position;
import utils.Utils;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * This class functions as a Jason environment, using EISMASSim to connect to a MASSim server.
 * (see http://cig.in.tu-clausthal.de/eis)
 * (see also https://multiagentcontest.org)
 *
 * @author Jomi
 * - adapted by ta10
 */
public class EISAdapter extends Environment implements AgentListener {

    private static final Atom PERCEPT_NAMESPACE_ATOM = new Atom("percept");
    private Logger logger = Logger.getLogger("EISAdapter." + EISAdapter.class.getName());

    private static EISAdapter singleton;
    private EnvironmentInterface ei;
    private SynchronizedPerceptWatcher perceptWatcher;

    public static Atom NONE = ASSyntax.createAtom("none");
    public static Atom BLOCK_ONE = ASSyntax.createAtom("block");
    public static Atom BLOCK_TWO = ASSyntax.createAtom("block_two");


    public EISAdapter() {
        super(1);
        singleton = this;
    }

    public static EISAdapter getSingleton() {
        return singleton;
    }

    @Override
    public void init(String[] args) {

        ei = new EnvironmentInterface("conf/eismassimconfig.json");
        perceptWatcher = SynchronizedPerceptWatcher.getInstance();

        try {
            ei.start();
        } catch (ManagementException e) {
            e.printStackTrace();
        }

        for (String e : ei.getEntities()) {
            System.out.println("Register agent " + e);

            try {
                ei.registerAgent(e);
            } catch (AgentException e1) {
                e1.printStackTrace();
            }

            ei.attachAgentListener(e, this);

            try {
                ei.associateEntity(e, e);
            } catch (RelationException e1) {
                e1.printStackTrace();
            }
        }

        perceptWatcher.start();
    }

    @Override
    public void handlePercept(String agent, Percept percept) {
        // We do not use notifications.
//        System.out.println("");
    }

    public AgentContainer getAgentContainer(String agentName) {
        return perceptWatcher.getAgentContainer(agentName);
    }

    @Override
    public List<Literal> getPercepts(String agName) {
        Structure strcEnt = ASSyntax.createStructure("entity", ASSyntax.createAtom(agName));

        Collection<Literal> ps = super.getPercepts(agName);
        List<Literal> percepts = ps == null ? new ArrayList<>() : new ArrayList<>(ps);

        if (ei == null) {
            throw new RuntimeException("Failed to get environment.");
        }

        clearPercepts(agName);

        if (agName.equals("df"))
            return percepts;

        // The operator should rely on it's own beliefs and internal actions.
        if (agName.equals("operator")) {
            percepts.add(ASSyntax.createLiteral("step", new NumberTermImpl(perceptWatcher.getSharedPerceptContainer().getStep())));
            return percepts;
        }

        AgentContainer agentContainer = getAgentContainer(agName);

        if (agentContainer == null)
            throw new RuntimeException("Failed to get agent container for: " + agName);


        for (Position position : agentContainer.getAttachedPositions()) {
            percepts.add(ASSyntax.createLiteral(PERCEPT_NAMESPACE_ATOM, "betterAttach", new NumberTermImpl(position.getX()), new NumberTermImpl(position.getY())));
        }

        List<Literal> agentPercepts = agentContainer.getCurrentPerceptions();

        Literal perceptLit = perceptToLiteral(agentContainer.getAgentLocation()).addAnnots(strcEnt);
        percepts.add(perceptLit);

        for (Literal p : agentPercepts) {
            p = mapEntityPerceptions(agentContainer, p);

            // Do not include perceptions that are filtered out
            if (p == null)
                continue;

            percepts.add(p);
        }

        // Add team mate relative perceptions
        percepts.addAll(addAuthenticatedTeammates(agName));

        getEpistemicEasyPercepts(agentContainer,percepts);
        //getEpistemicLocPercepts(agentContainer,percepts);
        //getEpistemicPercepts(agentContainer, percepts);


        return percepts;
    }

    private List<Literal> getEpistemicEasyPercepts(AgentContainer container, List<Literal> percepts) {


        // Get percepts used for epistemic reasoner
        var mapPercepts = getMapPercepts(container, Set.of(
                new Position(0, 1),
                new Position(0, -1),
                new Position(1, 0),
                new Position(-1, 0)
        ));

        List<Literal> newLits = new ArrayList<>();
        for(var entry : mapPercepts.entrySet())
        {
            var position = entry.getKey();
            var percept = entry.getValue();

            newLits.add(ASSyntax.createLiteral(LiteralImpl.LPos, "location",
                    ASSyntax.createNumber(position.getX()), ASSyntax.createNumber(position.getY()), percept.hasBlock() ? BLOCK_ONE : NONE));
        }

        percepts.addAll(newLits);
        return percepts;
    }


    private List<Literal> getEpistemicLocPercepts(AgentContainer container, List<Literal> percepts) {


        var mapPercepts = getMapPercepts(container, Set.of(
                new Position(0, 1),
                new Position(0, -1),
                new Position(1, 0),
                new Position(-1, 0)
        ));

        var possiblePositions = container.getAgentMap().findMatchingPerceptions(mapPercepts);
        var possibleAbsPos = possiblePositions.stream().filter(p -> p.getX() <= 5 && p.getX() >= 0 && p.getY() >= 0 && p.getY() <= 5).collect(Collectors.toList());

        if(possibleAbsPos.isEmpty())
            return percepts;

        Set<Position> notPosition = new HashSet<>();

        for (int i = 0; i <= 5; i++) {
            for (int j = 0; j <= 5; j++) {
                notPosition.add(new Position(i, j));
            }
        }

        notPosition.removeAll(possibleAbsPos);

        List<Literal> newLits = new ArrayList<>();
        for(Position p : notPosition)
        {
            newLits.add(ASSyntax.createLiteral(LiteralImpl.LNeg, "loc", ASSyntax.createNumber(p.getX()), ASSyntax.createNumber(p.getY())));
        }
//        var possibleSurroundings = getPossibleSurroundings(container, mapPercepts, Set.of(
//                new Position(1, 1)
//        ));

        // These are the surroundings that we KNOW
//        for (var perceptEntry : possibleSurroundings.entrySet()) {
//            Position relative = perceptEntry.getKey();
//            var perceptMap = perceptEntry.getValue();
//
//            // Do not add self
//            if(relative.isZeroPosition())
//                continue;
//
//            Set<Atom> possibleAtoms = new HashSet<>();
//
//
//            // Impossibilities -> Right now we only look at block or no block
//            Set<Atom> notPossibleAtoms = new HashSet<>();
//            notPossibleAtoms.add(BLOCK_ONE);
//            notPossibleAtoms.add(BLOCK_TWO);
//            notPossibleAtoms.add(NONE);
//
//            for(MapPercept percept : perceptMap.values())
//            {
//                if(percept.hasBlock())
//                    possibleAtoms.add(percept.getBlock().getDetails().equals("b0") ? BLOCK_ONE : BLOCK_TWO);
//                else
//                    possibleAtoms.add(NONE);
//            }
//
//            notPossibleAtoms.removeAll(possibleAtoms);
//
////            for(Atom possible : notPossibleAtoms)
////                percepts.add(ASSyntax.createLiteral(LiteralImpl.LNeg, "location", ASSyntax.createNumber(relative.getX()), ASSyntax.createNumber(relative.getY()), possible));
//        }
        // Better way to represent possibility here rather than inverse of set?
//        for (var surroundingEntry : possibleSurroundings.entrySet()) {
//            var location = surroundingEntry.getKey();
//            var possiblePercepts = surroundingEntry.getValue();
//            var possibleItems = new ArrayList<Percept.Item>();
//            possiblePercepts.forEach(per -> possibleItems.add(per.item));
//
//            // We use the inverted set to negate the impossible items
//            var invSet = new HashSet<>(Percept.Item.valueSet());
//            invSet.removeAll(possibleItems);
//
//            for(Percept.Item item : invSet) {
//                percepts.add(ASSyntax.createLiteral("location", ASSyntax.createNumber(location.getFirst()), ASSyntax.createNumber(location.getSecond()), item.item).setNegated(Literal.LNeg));
//            }
//        }
        percepts.addAll(newLits);
        return percepts;
    }

    private void getEpistemicPercepts(AgentContainer container, List<Literal> percepts) {
        var mapPercepts = getMapPercepts(container, Set.of(
                new Position(0, 1),
                new Position(0, -1),
                new Position(1, 0),
                new Position(-1, 0)
        ));
        var possibleSurroundings = getPossibleSurroundings(container, mapPercepts, Set.of(
                new Position(1, 1)
        ));

        // These are the surroundings that we KNOW
        for (var perceptEntry : possibleSurroundings.entrySet()) {
            Position relative = perceptEntry.getKey();
            var perceptMap = perceptEntry.getValue();

            // Do not add self
            if(relative.isZeroPosition())
                continue;

            Set<Atom> possibleAtoms = new HashSet<>();


            // Impossibilities -> Right now we only look at block or no block
            Set<Atom> notPossibleAtoms = new HashSet<>();
            notPossibleAtoms.add(BLOCK_ONE);
            notPossibleAtoms.add(BLOCK_TWO);
            notPossibleAtoms.add(NONE);

            for(MapPercept percept : perceptMap.values())
            {
                if(percept.hasBlock())
                    possibleAtoms.add(percept.getBlock().getDetails().equals("b0") ? BLOCK_ONE : BLOCK_TWO);
                else
                    possibleAtoms.add(NONE);
            }

            notPossibleAtoms.removeAll(possibleAtoms);

//            for(Atom possible : notPossibleAtoms)
//                percepts.add(ASSyntax.createLiteral(LiteralImpl.LNeg, "location", ASSyntax.createNumber(relative.getX()), ASSyntax.createNumber(relative.getY()), possible));
        }
        // Better way to represent possibility here rather than inverse of set?
//        for (var surroundingEntry : possibleSurroundings.entrySet()) {
//            var location = surroundingEntry.getKey();
//            var possiblePercepts = surroundingEntry.getValue();
//            var possibleItems = new ArrayList<Percept.Item>();
//            possiblePercepts.forEach(per -> possibleItems.add(per.item));
//
//            // We use the inverted set to negate the impossible items
//            var invSet = new HashSet<>(Percept.Item.valueSet());
//            invSet.removeAll(possibleItems);
//
//            for(Percept.Item item : invSet) {
//                percepts.add(ASSyntax.createLiteral("location", ASSyntax.createNumber(location.getFirst()), ASSyntax.createNumber(location.getSecond()), item.item).setNegated(Literal.LNeg));
//            }
//        }
    }

    /**
     * Gets surrounding percepts.
     *
     * @param container
     * @return
     */
    private Map<Position, MapPercept> getMapPercepts(AgentContainer container, Set<Position> positions) {
        // Add fake map percepts
        addFakePercepts(container);


        // Filter non-self percepts
        return container.getAgentMap().getCurrentStepChunks().stream()
                // Filter out self perception
                .filter(mapPercept -> positions.contains(container.absoluteToRelativeLocation(mapPercept.getLocation())))

                // Create map from perception
                .collect(Collectors.toMap(mapPercept -> container.absoluteToRelativeLocation(mapPercept.getLocation()), mapPercept -> mapPercept));
    }

    private void addFakePercepts(AgentContainer container) {

    }

    // {Rel Position -> {Abs Position -> Percept}}
    private Map<Position, Map<Position, MapPercept>> getPossibleSurroundings(AgentContainer container, Map<Position, MapPercept> mapPercepts, Set<Position> possiblePos) {
        // Find all chunks in map that match the list of percepts
        Map<Position, Map<Position, MapPercept>> possibleSurroundings = new HashMap<>();

        for (Position position : container.getAgentMap().findMatchingPerceptions(mapPercepts)) {
            for (Position relPos : possiblePos) {
                if (!possibleSurroundings.containsKey(relPos))
                    possibleSurroundings.put(relPos, new HashMap<>());

                var posSurr = possibleSurroundings.get(relPos);

                Position calc = relPos.add(position);
                Position relCalc = container.absoluteToRelativeLocation(calc);

                if(mapPercepts.containsKey(relCalc))
                    continue;

                MapPercept percept = container.getAgentMap().getMapPercept(calc);

                // Percept is null
                if (percept == null)
                    continue;

                posSurr.put(calc, percept);
            }
        }

        possibleSurroundings.entrySet().removeIf(e -> e.getValue().isEmpty());

        return possibleSurroundings;
    }


    private synchronized List<Literal> addAuthenticatedTeammates(String entity) {
        AgentContainer agentContainer = getAgentContainer(entity);
        Map<AgentContainer, Position> agentLocations = agentContainer.getAgentAuthentication().getAuthenticatedTeammatePositions();


        return agentLocations.entrySet().stream().map((entry) -> {
            AgentContainer otherAgentContainer = entry.getKey();
            Position position = entry.getValue();

            // Convert
            Position relativePosition = agentContainer.absoluteToRelativeLocation(position);
            return ASSyntax.createLiteral(PERCEPT_NAMESPACE_ATOM, "teamAgent", new NumberTermImpl(relativePosition.getX()), new NumberTermImpl(relativePosition.getY()), new Atom(otherAgentContainer.getAgentName()));

        }).collect(Collectors.toList());
    }


    public AgentMap getAgentMap(String agentName) {
        return getAgentContainer(agentName).getAgentMap();
    }

    private synchronized Literal mapEntityPerceptions(AgentContainer agentContainer, Literal perceptLiteral) {
        if (perceptLiteral == null)
            return null;

        String perceptName = perceptLiteral.getFunctor();

        if (!perceptName.equals("thing"))
            return perceptLiteral;

        String thingType = LiteralUtils.GetStringParameter(perceptLiteral, 2);

        if (!thingType.equals("entity"))
            return perceptLiteral;

        String entity = agentContainer.getAgentName();

        String team = LiteralUtils.GetStringParameter(perceptLiteral, 3);

        // Other team perception
        if (!team.equals(agentContainer.getAgentPerceptContainer().getSharedPerceptContainer().getTeamName()))
            return perceptLiteral;

        Position relativePosition = new Position(LiteralUtils.GetNumberParameter(perceptLiteral, 0).intValue(), LiteralUtils.GetNumberParameter(perceptLiteral, 1).intValue());

        if (relativePosition.equals(new Position(0, 0)))
            // Modify self perception
            return ASSyntax.createLiteral("self", new NumberTermImpl(relativePosition.getX()), new NumberTermImpl(relativePosition.getY()), new Atom(entity));

        Position perceptAbsolutePosition = agentContainer.relativeToAbsoluteLocation(relativePosition);

        List<Position> matchingAgents = agentContainer.getAgentAuthentication().getAuthenticatedTeammatePositions()
                .values()
                .stream()
                .filter(absTeamPos -> absTeamPos.equals(perceptAbsolutePosition)).collect(Collectors.toList());

        int numberOfMatchingAgents = matchingAgents.size();

        if (numberOfMatchingAgents == 1)
            return null; // Filter out team percepts, as we add them in later

        else if (numberOfMatchingAgents > 1) {
            logger.warning("There are multiple agents with the same translation coordinates for " + entity);
            logger.warning("Authenticated Agents: " + agentContainer.getAgentAuthentication().getAuthenticatedAgents());
            return null;
        }

        return perceptLiteral;
    }


    private void setAuthenticatedAgent(String agent1, String agent2, Position pos) {
        if (agent1.equals(agent2)) {
            logger.info("Attempting to authenticate the same agent.");
            return;
        }

        AgentContainer agentContainer1 = getAgentContainer(agent1);
        AgentContainer agentContainer2 = getAgentContainer(agent2);

        if (pos != null) {
            agentContainer1.getAgentAuthentication().authenticateAgent(agentContainer2, pos);
            agentContainer2.getAgentAuthentication().authenticateAgent(agentContainer1, pos.negate());
        }

    }

    public void authenticateAgent(String agentName, Position agentPosition, String otherAgentName, Position otherAgentPosition, Position relativePerception) {
        // Calculate the translation value
        Position translation = agentPosition.add(relativePerception).subtract(otherAgentPosition);
        setAuthenticatedAgent(agentName, otherAgentName, translation);

    }

    private void resetOperator(RuntimeServices runtimeServices) {
        String operator = "operator";
        runtimeServices.killAgent(operator, operator, 0);

        try {
            String newAg = runtimeServices.createAgent(operator, "operator.asl", null, null, null, null, null);
            if (!newAg.equals(operator))
                throw new NullPointerException();
        } catch (Exception e) {
            e.printStackTrace();
        }
        runtimeServices.startAgent(operator);
    }

    private void resetAgents(RuntimeServices runtimeServices) {
        runtimeServices.getAgentsNames().forEach(a -> {

            try {
                if (!a.contains("agent"))
                    return;

                runtimeServices.killAgent(a, a, 0);
                runtimeServices.createAgent(a, "agentA.asl", "eis.functions.CustomAgent", null, null, null, null);
                runtimeServices.startAgent(a);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public boolean executeAction(String agName, Structure action) {

        if (ei == null) {
            logger.warning("There is no environment loaded! Ignoring action " + action);
            return false;
        }


        if (action.getFunctor().equals("resetAgents")) {
//            CentralisedRuntimeServices runtimeServices = (CentralisedRuntimeServices) super.getEnvironmentInfraTier().getRuntimeServices();
//            resetOperator(runtimeServices);
//            resetAgents(runtimeServices);
            //Doesnt work rn
            return true;
        }

        if (action.getFunctor().equalsIgnoreCase("authenticateAgents")) {

            return false;
        }

        if (action.getFunctor().equalsIgnoreCase("taskSubmitted")) {
            AgentContainer ent = getAgentContainer(agName);
            ent.taskSubmitted();

            return true;
        }

        if (action.getFunctor().equalsIgnoreCase("blockAttached")) {
            AgentContainer ent = getAgentContainer(agName);
            Position relativePos = null;

            if (action.getArity() == 1) {
                Literal dirLiteral = (Literal) action.getTerm(0);
                Direction dir = Utils.DirectionStringToDirection(dirLiteral.getFunctor());

                if (dir == null)
                    return false;

                relativePos = dir.getPosition();
            } else if (action.getArity() == 2) {

                int x = LiteralUtils.GetNumberParameter(action, 0).intValue();
                int y = LiteralUtils.GetNumberParameter(action, 1).intValue();
                relativePos = new Position(x, y);
            }

            Position absolutePos = ent.relativeToAbsoluteLocation(relativePos);
            MapPercept mapPercept = ent.getAgentMap().getMapPercept(absolutePos);
            if (mapPercept == null || !mapPercept.hasBlock())
                return false;

            Block block = mapPercept.getBlock();

            if (block == null)
                return false;

            //   ent.attachBlock(relativePos);

            return true;
        }

        if (action.getFunctor().equalsIgnoreCase("blockDetached")) {
            AgentContainer ent = getAgentContainer(agName);
            Literal dirLiteral = (Literal) action.getTerm(0);
            Direction dir = Utils.DirectionStringToDirection(dirLiteral.getFunctor());
            MapPercept mapPercept = getAgentContainer(agName).getAgentMap().getRelativePerception(dir);

            if (dir == null || mapPercept == null || !mapPercept.hasBlock())
                return false;

            Block block = mapPercept.getBlock();

            if (block == null)
                return false;

            ent.detachBlock(dir.getPosition());

            return true;
        }

        if (action.getFunctor().equals("addForbiddenDirection")) {
            Atom direction = (Atom) action.getTerm(0);
            Position dirPos = Utils.DirectionStringToDirection(direction.getFunctor()).getPosition();
            getAgentMap(agName).addForbiddenLocation(dirPos);
            return true;

        }

        try {
            System.err.println(action.getFunctor());
            ei.performAction(agName, literalToAction(action));
//            ei.performAction(agName, new Action("skip"));
            return true;
        } catch (ActException e) {
            e.printStackTrace();
        }

        return false;
    }

    private static int getNumberTermInt(Term t) {
        return (int) ((NumberTermImpl) t).solve();
    }

    /**
     * Called before the end of MAS execution
     */
    @Override
    public void stop() {
        if (ei != null) {
            try {
                if (ei.isKillSupported()) ei.kill();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.stop();
    }

    public static Literal perceptToLiteral(Atom namespace, Percept per) {
        Literal l;
        if (namespace == null)
            l = ASSyntax.createLiteral(per.getName());
        else
            l = ASSyntax.createLiteral(namespace, per.getName());

        for (Parameter par : per.getParameters())
            l.addTerm(parameterToTerm(par));
        return l;
    }

    public static Literal perceptToLiteral(Percept per) {
        return perceptToLiteral(PERCEPT_NAMESPACE_ATOM, per);
    }

    public static Term parameterToTerm(Parameter par) {
        if (par instanceof Numeral) {
            return ASSyntax.createNumber(((Numeral) par).getValue().doubleValue());
        } else if (par instanceof Identifier) {
            try {
                Identifier i = (Identifier) par;
                String a = i.getValue();
                if (!Character.isUpperCase(a.charAt(0)))
                    return ASSyntax.parseTerm(a);
            } catch (Exception ignored) {
            }
            return ASSyntax.createString(((Identifier) par).getValue());
        } else if (par instanceof ParameterList) {
            ListTerm list = new ListTermImpl();
            ListTerm tail = list;
            for (Parameter p : (ParameterList) par)
                tail = tail.append(parameterToTerm(p));
            return list;
        } else if (par instanceof Function) {
            Function f = (Function) par;
            Structure l = ASSyntax.createStructure(f.getName());
            for (Parameter p : f.getParameters())
                l.addTerm(parameterToTerm(p));
            return l;
        }
        throw new RuntimeException("The type of parameter " + par + " is unknown!");
    }

    public static Action literalToAction(Literal action) {
        Parameter[] pars = new Parameter[action.getArity()];
        for (int i = 0; i < action.getArity(); i++)
            pars[i] = termToParameter(action.getTerm(i));
        return new Action(action.getFunctor(), pars);
    }

    public static Parameter termToParameter(Term t) {
        if (t.isNumeric()) {
            try {
                double d = ((NumberTerm) t).solve();
                if ((d == Math.floor(d)) && !Double.isInfinite(d)) return new Numeral((int) d);
                return new Numeral(d);
            } catch (NoValueException e) {
                e.printStackTrace();
            }
            return new Numeral(null);
        } else if (t.isList()) {
            Collection<Parameter> terms = new ArrayList<>();
            for (Term listTerm : (ListTerm) t)
                terms.add(termToParameter(listTerm));
            return new ParameterList(terms);
        } else if (t.isString()) {
            return new Identifier(((StringTerm) t).getString());
        } else if (t.isLiteral()) {
            Literal l = (Literal) t;
            if (!l.hasTerm()) {
                return new Identifier(l.getFunctor());
            } else {
                Parameter[] terms = new Parameter[l.getArity()];
                for (int i = 0; i < l.getArity(); i++)
                    terms[i] = termToParameter(l.getTerm(i));
                return new Function(l.getFunctor(), terms);
            }
        }
        return new Identifier(t.toString());
    }

    public EnvironmentInterface getEnvironmentInterface() {
        return ei;
    }
}
