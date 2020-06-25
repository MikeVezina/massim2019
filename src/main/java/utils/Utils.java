package utils;

import map.Direction;
import map.Position;
import jason.NoValueException;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;

import java.util.ArrayList;
import java.util.logging.Logger;

public class Utils {
    public static String RelativeLocationToDirection(int x, int y) {
        // Check X value if Y value is 0
        if (y == 0) {
            switch (x) {
                case -1:
                    return "w";
                case 1:
                    return "e";
                default:
                    break;
            }
        } else if (x == 0) {
            // Check Y value if X == 0
            switch (y) {
                case -1:
                    return "n";
                case 1:
                    return "s";
                default:
                    break;
            }
        }

        return "";
    }

    public static String RelativeLocationToDirection(Position location) {
        return RelativeLocationToDirection(location.getX(), location.getY());
    }

    public static Direction DirectionStringToDirection(String direction) {

        if (direction.equalsIgnoreCase(Direction.WEST.getAtom().getFunctor()))
            return Direction.WEST;
        else if (direction.equalsIgnoreCase(Direction.EAST.getAtom().getFunctor()))
            return Direction.EAST;
        else if (direction.equalsIgnoreCase(Direction.NORTH.getAtom().getFunctor()))
            return Direction.NORTH;
        else if (direction.equalsIgnoreCase(Direction.SOUTH.getAtom().getFunctor()))
            return Direction.SOUTH;

        return null;
    }

    public static double SolveNumberTerm(Term t) {
        if (!(t instanceof NumberTermImpl))
            throw new NullPointerException("Term is not a NumberTermImpl");
        try {
            return ((NumberTerm) t).solve();
        } catch (NoValueException nVe) {
            nVe.printStackTrace();
            throw new NullPointerException("Failed to solve number term.");

        }
    }

    public static void DumpIntentionStack(TransitionSystem ts) {
        if (ts == null || ts.getC() == null || ts.getC().getSelectedIntention() == null)
            return;
        ts.getLogger().info("Intention Stack Dump: " + ts.getC().getSelectedIntention());
    }

    public static double Distance(Integer xArg, Integer yArg) {
        return Math.sqrt(Math.pow(xArg, 2) + Math.pow(yArg, 2));
    }

    public static class Area extends ArrayList<Position> {
        private static Logger LOG = Logger.getLogger(Area.class.getName());

        /**
         * Creates a new list containing all positions belonging to the
         * area around a given center within the given radius.
         */
        public Area(Position center, int radius) {
            for (var dx = -radius; dx <= radius; dx++) {
                var x = center.getX() + dx;
                var dy = radius - Math.abs(dx);
                for (var y = center.getY() - dy; y <= center.getY() + dy; y++) {
                    // LOG.info("Creating Position: " + new Position(x, y));
                    this.add(new Position(x, y));
                }
            }
        }
    }
}
