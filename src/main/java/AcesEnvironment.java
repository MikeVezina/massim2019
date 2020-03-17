import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Structure;
import jason.environment.Environment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AcesEnvironment extends Environment {

    private Stack<String> cardDeck = new Stack<>();
    private Map<String, String> agentDeal = new ConcurrentHashMap<>();
    private Map<String, String> agentNames = new ConcurrentHashMap<>();
    private WorldRequest worldRequest;
    private int turn = 0;


    public AcesEnvironment() {
        super(20);


        agentNames.put("acesAndEights1", "Alice");
        agentNames.put("acesAndEights2", "Bob");
        agentNames.put("acesAndEights3", "Carl");

        cardDeck.add("A");
        cardDeck.add("A");
        cardDeck.add("A");
        cardDeck.add("A");

        cardDeck.add("E");
        cardDeck.add("E");
        cardDeck.add("E");
        cardDeck.add("E");

        Collections.shuffle(cardDeck);

    }

    private synchronized void dealCards() {
        if (!agentDeal.isEmpty())
            return;

        List<String> cards = new ArrayList<>();


        for (String agentName : super.getEnvironmentInfraTier().getRuntimeServices().getAgentsNames()) {
            String actualName = agentNames.get(agentName);

            agentDeal.compute(actualName, (agent, lastVal) -> {
                StringBuilder newCards = new StringBuilder();
                List<String> cardsTemp = new ArrayList<>();
                cardsTemp.add(cardDeck.pop());
                cardsTemp.add(cardDeck.pop());
                cardsTemp.sort(Comparator.naturalOrder());

                for(String c : cardsTemp)
                    newCards.append(c);

                cards.add(newCards.toString());
                return newCards.toString();
            });
        }

        this.worldRequest = new WorldRequest(cards.get(0), cards.get(1), cards.get(2));
    }

    @Override
    public List<Literal> getPercepts(String agName) {
        Collection<Literal> ps = super.getPercepts(agName);
        List<Literal> percepts = ps == null ? new ArrayList<>() : new ArrayList<>(ps);

        if (agentDeal.isEmpty())
            dealCards();

        clearPercepts(agName);

        String actualName = agentNames.get(agName);


        if(turn==0 && actualName.equals("Alice") || turn==1 && actualName.equals("Bob") || turn==2 && actualName.equals("Carl"))
            percepts.add(ASSyntax.createAtom("turn"));

        percepts.add(ASSyntax.createLiteral("action", ASSyntax.createString(this.worldRequest.getAgentAction(actualName))));

        if(actualName.equals("Alice"))
        {
            if(worldRequest.modelCheckFormula("(K alice alice_AA)"))
            {
                percepts.add(ASSyntax.createLiteral("card(AA)"));
            }
            if(worldRequest.modelCheckFormula("(K alice alice_EE)"))
            {
                percepts.add(ASSyntax.createLiteral("card(EE)"));
            }

            if(worldRequest.modelCheckFormula("(K alice alice_AE)"))
            {
                percepts.add(ASSyntax.createLiteral("card(AE)"));
            }
        }

        for (Map.Entry<String, String> agentCardEntry : agentDeal.entrySet()) {
            if (agentCardEntry.getKey().equals(actualName))
                continue;

            String name = agentCardEntry.getKey();
            String cards = agentCardEntry.getValue();

            percepts.add(ASSyntax.createLiteral("card", ASSyntax.createAtom(name), ASSyntax.createAtom(cards)));
        }
        return percepts;
    }

    @Override
    public boolean executeAction(String agName, Structure act) {
        if(act.getFunctor().equals("announce"))
        {
            System.out.println( agName + " Announcing");
            String action = ((StringTerm)act.getTerm(0)).getString();
            worldRequest.performAction(action);
            turn = (turn + 1) % 3;
            return true;
        }

        return super.executeAction(agName, act);
    }

    /**
     * Called before the end of MAS execution
     */
    @Override
    public void stop() {
        super.stop();
    }


}
