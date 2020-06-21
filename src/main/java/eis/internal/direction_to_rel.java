package eis.internal;

import jason.JasonException;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Atom;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;
import map.Position;
import utils.Utils;

public class direction_to_rel extends DefaultInternalAction {

	private static final long serialVersionUID = -6214881485708125130L;

	@Override
	public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {

		// execute the internal action

		ts.getAg().getLogger().fine("executing internal action 'UxVInternalActions.absolutePosition'");
		
		
		try {
			// Get the parameters
			String direction = ((Atom) args[0]).getFunctor();

			Position relLocation = Utils.DirectionStringToDirection(direction).getPosition();
			
			// Create the result term
			NumberTerm resultX = new NumberTermImpl(relLocation.getX());
			NumberTerm resultY = new NumberTermImpl(relLocation.getY());

			// Unify
			boolean relPositionX = un.unifies(resultX, args[1]);
			boolean relPositionY = un.unifies(resultY, args[2]);

			// Return result
			return (relPositionX && relPositionY);
		}
		// Deal with error cases
		catch (ArrayIndexOutOfBoundsException e) {
			throw new JasonException("The internal action 'DirectionToRelativePosition' received the wrong number of arguments.");
		} catch (ClassCastException e) {
			throw new JasonException(
					"The internal action 'DirectionToRelativePosition' received arguments that are of the wrong type.");
		} catch (Exception e) {
			throw new JasonException("Error in 'DirectionToRelativePosition'.");
		}
	}
}