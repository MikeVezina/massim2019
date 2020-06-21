package eis.internal;

import eis.EISAdapter;
import eis.agent.AgentContainer;
import eis.percepts.requirements.Requirement;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import map.Direction;
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


        var att = agentContainer.getAttachedPositions();
        System.out.println(att);

        // Only keep the requirements we currently have
        oldReq.removeIf(r -> !agentContainer.getAttachedPositions().contains(r.getPosition()));

        if (oldReq.size() != agentContainer.getAttachedPositions().size()) {
            ts.getLogger().info("Somethings wrong here?");
        }

        ts.getLogger().info("==== Find Next Req: " + taskName + " ====");
        ts.getLogger().info("All Reqs: " + remainingRequirements);
        ts.getLogger().info("Old Reqs: " + oldReq);

        remainingRequirements.removeIf(oldReq::contains);
        ts.getLogger().info("Remaining Reqs: " + remainingRequirements);

        if (oldReq.isEmpty())
            return false;

        Requirement lastRemoved = oldReq.removeLast();

        // We have no more remaining reqs (finished task)
        // We still want to unify the previous req.
        if (remainingRequirements.isEmpty()) {
            // Get the last requirement that was attached
            boolean lastRemovedUnifies = un.unifies(args[1], reqToStruct(lastRemoved));
            return un.unifies(args[2], ASSyntax.createAtom("done")) && lastRemovedUnifies;
        }

        Requirement next = remainingRequirements.peek();

        // Find an attachable requirement block
        while(lastRemoved != null && Direction.GetDirection(next.getPosition().subtract(lastRemoved.getPosition())).equals(Direction.NONE))
            lastRemoved = oldReq.removeLast();

        if(lastRemoved == null)
            throw new RuntimeException("Failed to find the last removed block");

        boolean lastRemovedUnifies = un.unifies(args[1], reqToStruct(lastRemoved));
        ts.getLogger().info("Prev. Attach: " + lastRemoved);
        ts.getLogger().info("Next: " + next);
        return un.unifies(args[2], reqToStruct(next)) && lastRemovedUnifies;
    }

    private Structure reqToStruct(Requirement requirement) {
        Term xArg = ASSyntax.createNumber(requirement.getPosition().getX());
        Term yArg = ASSyntax.createNumber(requirement.getPosition().getY());
        Term blockArg = ASSyntax.createAtom(requirement.getBlockType());

        return ASSyntax.createStructure("req", xArg, yArg, blockArg);
    }
}