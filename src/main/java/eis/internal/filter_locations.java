package eis.internal;

import eis.EISAdapter;
import eis.watcher.SynchronizedPerceptWatcher;
import jason.JasonException;
import map.AgentMap;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import map.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class filter_locations extends DefaultInternalAction {

    private static final long serialVersionUID = -6214881485708125130L;
    private static final Atom NORTH = new Atom("n");
    private static final Atom SOUTH = new Atom("s");
    private static final Atom WEST = new Atom("w");
    private static final Atom EAST = new Atom("e");

    @Override
    protected void checkArguments(Term[] args) throws JasonException {
        super.checkArguments(args);
        if(args.length != 2)
            throw JasonException.createWrongArgumentNb(this);

        if(!(args[0] instanceof ListTerm))
            throw JasonException.createWrongArgument(this, "first term is not list");
    }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        ListTerm prevPossibilities = (ListTerm) args[0];

        var container = SynchronizedPerceptWatcher.getInstance().getAgentContainer(ts.getAgArch().getAgName());
        if(container.getAgentPerceptContainer().getLastAction().equals("move") && container.getAgentPerceptContainer().getLastActionResult().equals("success"))
        {
            var dir = Arrays.stream(Direction.validDirections()).filter(d -> d.getAtom().getFunctor().equals(container.getAgentPerceptContainer().getLastActionParams().get(0).toProlog())).findFirst().get();
            for(Term posTerm : prevPossibilities)
            {
                LiteralImpl pos = (LiteralImpl) posTerm;
                double oldX = ((NumberTerm)pos.getTerm(0)).solve();
                double oldY = ((NumberTerm)pos.getTerm(1)).solve();
                pos.setTerm(0, new NumberTermImpl(oldX + dir.getPosition().getX()));
                pos.setTerm(1, new NumberTermImpl(oldY + dir.getPosition().getY()));
            }

        }


//        return truetrue;
        return un.unifies(prevPossibilities, args[1]);
    }

}
