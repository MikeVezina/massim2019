package eis.internal;

import eis.EISAdapter;
import eis.agent.AgentContainer;
import eis.agent.Rotation;
import eis.watcher.SynchronizedPerceptWatcher;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import map.MapPercept;
import map.Position;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class fix_shared_attachment extends DefaultInternalAction {

    private static final long serialVersionUID = -6214865185708125130L;
    private static final String CLASS_NAME = fix_shared_attachment.class.getName();

    private static AtomicLong lastUpdateStep = new AtomicLong(-1);
    private static ConcurrentHashMap<AgentContainer, Literal> doubleAttachMap;

    public fix_shared_attachment()
    {
        if(doubleAttachMap == null)
            doubleAttachMap = new ConcurrentHashMap<>();

        System.out.println();
    }


    /**
     * This internal action returns the attachments currently attached to us.
     * @param ts
     * @param un
     * @param args
     * @return
     */
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) {

        var agentContainer = SynchronizedPerceptWatcher.getInstance().getAgentContainer(ts.getUserAgArch().getAgName());

        if(lastUpdateStep.get() < agentContainer.getSharedPerceptContainer().getStep())
        {
            // Add other literals for coordinating attachments
            doubleAttachMap.clear();

            for(AgentContainer container : SynchronizedPerceptWatcher.getInstance().getAgentContainers())
            {
                container.getSharedAttachments().forEach(((position, otherAgent) -> {
                    var dropAttLit = ASSyntax.createLiteral("dropAttach", new NumberTermImpl(position.getX()), new NumberTermImpl(position.getY()));
                    var waitLit = ASSyntax.createLiteral( "waitDetach");

                    doubleAttachMap.put(container, dropAttLit);
                    doubleAttachMap.put(otherAgent, waitLit);
                }));
            }

            lastUpdateStep.set(agentContainer.getCurrentStep());
        }

        if(doubleAttachMap.containsKey(agentContainer))
            return un.unifies(args[0], doubleAttachMap.get(agentContainer));

        // Return but do not unify
        return true;
    }
}