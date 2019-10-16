package eis.internal;

import eis.EISAdapter;
import eis.agent.AgentContainer;
import eis.percepts.requirements.Requirement;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import map.MapPercept;
import map.Position;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

public class get_next_req extends DefaultInternalAction {

    private static final long serialVersionUID = -6214881485708125130L;
    private static final String CLASS_NAME = get_next_req.class.getName();

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) {
        AgentContainer agentContainer = EISAdapter.getSingleton().getAgentContainer(ts.getUserAgArch().getAgName());

        String taskName = ((Atom) args[0]).getFunctor();
        Queue<Requirement> remainingRequirements = agentContainer.getSharedPerceptContainer().getTaskMap().get(taskName).getPlannedRequirements();
                Deque<Requirement> oldReq = agentContainer.getSharedPerceptContainer().getTaskMap().get(taskName).getPlannedRequirements();


        // Remove any un-done reqs
        oldReq.removeIf(r -> !agentContainer.getAttachedPositions().contains(r.getPosition()));

        for (Position percept : agentContainer.getAttachedPositions()) {
            remainingRequirements.removeIf(req -> req.getPosition().equals(percept));
        }



        if(oldReq.isEmpty())
            return false;

        Requirement lastRemoved = oldReq.getLast();

        boolean lastRemovedUnifies = un.unifies(args[1], reqToStruct(lastRemoved));

        if (remainingRequirements.isEmpty())
            return un.unifies(args[2], ASSyntax.createAtom("done")) && lastRemovedUnifies;

        Requirement next = remainingRequirements.peek();
        return un.unifies(args[2], reqToStruct(next)) && lastRemovedUnifies;
    }

    private Structure reqToStruct(Requirement requirement) {
        Term xArg = ASSyntax.createNumber(requirement.getPosition().getX());
        Term yArg = ASSyntax.createNumber(requirement.getPosition().getY());
        Term blockArg = ASSyntax.createAtom(requirement.getBlockType());

        return ASSyntax.createStructure("req", xArg, yArg, blockArg);
    }
}