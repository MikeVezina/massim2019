package eis.internal;

import eis.EISAdapter;
import eis.agent.AgentContainer;
import eis.agent.Rotation;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import map.MapPercept;
import map.Position;

public class get_attached_blocks extends DefaultInternalAction {

    private static final long serialVersionUID = -6214881485708125130L;
    private static final String CLASS_NAME = get_attached_blocks.class.getName();

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) {
        AgentContainer agentContainer = EISAdapter.getSingleton().getAgentContainer(ts.getUserAgArch().getAgName());

        ListTerm attachedBlocks = new ListTermImpl();

        for(MapPercept percept : agentContainer.getAttachedPercepts())
        {
            Position relativePos = agentContainer.absoluteToRelativeLocation(percept.getLocation());
            Structure attachedStruct = ASSyntax.createStructure("attached", ASSyntax.createNumber(relativePos.getX()), ASSyntax.createNumber(relativePos.getY()), ASSyntax.createAtom(percept.getAttachableThing().getThingType()), ASSyntax.createAtom(percept.getAttachableThing().getDetails()));
            attachedBlocks.add(attachedStruct);
        }

        if(attachedBlocks.isEmpty())
            return false;

        return un.unifies(args[0], attachedBlocks);
    }
}