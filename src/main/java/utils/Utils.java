package utils;

import jason.JasonException;
import jason.NoValueException;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;
import jason.util.Pair;

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
        }
        else if (x == 0) {
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

    public static Position DirectionToRelativeLocation(String direction) {
        int x = 0;
        int y = 0;

        if (direction.equalsIgnoreCase("w"))
            x = -1;
        else if (direction.equalsIgnoreCase("e"))
            x = 1;
        else if (direction.equalsIgnoreCase("n"))
            y = -1;
        else if (direction.equalsIgnoreCase("s"))
            y = 1;
        else
            return null;

        return new Position(x, y);

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

    public static void DumpIntentionStack(TransitionSystem ts)
    {
        if(ts == null || ts.getC() == null || ts.getC().getSelectedIntention() == null)
            return;
        ts.getLogger().info("Intention Stack Dump: " + ts.getC().getSelectedIntention());
    }

    public static double Distance(Integer xArg, Integer yArg) {
        return Math.sqrt(Math.pow(xArg, 2) + Math.pow(yArg, 2));
    }
}
