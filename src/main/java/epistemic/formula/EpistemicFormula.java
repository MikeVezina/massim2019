package epistemic.formula;

import epistemic.wrappers.WrappedLiteral;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.Term;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This class is used to represent a knowledge formula.
 * For example, a literal 'know(know(hello))' will get unwrapped so that we have access
 * to the rootLiteral (i.e. hello) as well as the chain of embedded literals.
 */
public class EpistemicFormula {

    public enum EpistemicFormulaFunctor {
        KNOW("know"),
        POSSIBLE("possible");

        private final Set<String> functorSet;

        EpistemicFormulaFunctor(String... functors) {
            this.functorSet = new HashSet<>();
            this.functorSet.addAll(Arrays.asList(functors));
        }

        public boolean isFunctor(String functor) {
            return this.functorSet.contains(functor);
        }

        /**
         * Finds the first functor enum value with the given functor.
         *
         * @param functor The string functor to look for in enum values.
         * @return The first corresponding enum value or null if the functor could not be found.
         */
        public static EpistemicFormulaFunctor findFunctor(String functor) {
            for (EpistemicFormulaFunctor functorVal : EpistemicFormulaFunctor.values()) {
                if (functorVal.isFunctor(functor))
                    return functorVal;
            }
            return null;
        }
    }

    private WrappedLiteral rootLiteral;
    private final WrappedLiteral originalLiteral;
    private EpistemicFormula nextLiteral;


    /**
     * Can only be constructed through the static parseLiteral method.
     *
     * @param originalLiteral The original literal corresponding to this epistemic formula
     */
    private EpistemicFormula(Literal originalLiteral) {
        this.originalLiteral = new WrappedLiteral(originalLiteral);
    }

    /**
     * Recursively parses a literal into an epistemic formula. If the literal is not
     * an epistemic literal, an EpistemicFormula object will not be created and
     * null will be returned. Calls {@link EpistemicFormula#isEpistemicLiteral(Literal)}
     * to check if literal is epistemic.
     *
     * @param originalLiteral The epistemic literal to be converted into an EpistemicFormula object.
     * @return An EpistemicFormula object parsed from the literal. Null if the literal is not epistemic.
     * @see EpistemicFormula#isEpistemicLiteral(Literal)
     */
    public static EpistemicFormula fromLiteral(Literal originalLiteral) {
        return isEpistemicLiteral(originalLiteral) ? parseNextLiteralRecursive(originalLiteral.copy()) : null;
    }

    static EpistemicFormula parseNextLiteralRecursive(Literal currentLiteral) {

        var currentEpistemicFormula = new EpistemicFormula(currentLiteral);

        if (!isEpistemicLiteral(currentLiteral)) {

            currentEpistemicFormula.rootLiteral = new WrappedLiteral(currentLiteral);
            currentEpistemicFormula.nextLiteral = null;

            return currentEpistemicFormula;
        }

        Term nestedTerm = currentLiteral.getTerm(0);

        if (!(nestedTerm instanceof Literal))
            throw new IllegalArgumentException("currentLiteral (" + currentLiteral + ") does not have a nested literal term");

        currentEpistemicFormula.setNextLiteral(parseNextLiteralRecursive((Literal) nestedTerm));
        return currentEpistemicFormula;
    }


    public WrappedLiteral getRootLiteral() {
        return rootLiteral;
    }

    public EpistemicFormula getNextFormula() {
        return nextLiteral;
    }

    private void setNextLiteral(EpistemicFormula nextLiteral) {
        this.rootLiteral = nextLiteral.rootLiteral;
        this.nextLiteral = nextLiteral;
    }

    public static boolean isEpistemicLiteral(Literal curLit) {
        return curLit != null && EpistemicFormulaFunctor.findFunctor(curLit.getFunctor()) != null && curLit.getArity() == 1;
    }

    public Literal getCleanedOriginal() {
        return originalLiteral.getCleanedLiteral();
    }

    public WrappedLiteral getOriginalWrappedLiteral() {
        return originalLiteral;
    }

    /**
     * Applies the unifier to the current formula
     * @param unifier The unifier with the corresponding variable values
     * @return A new epistemic formula object with the unified values.
     */
    public EpistemicFormula capply(Unifier unifier)
    {
        Literal applied = (Literal) getCleanedOriginal().capply(unifier);
        applied.resetHashCodeCache();
        return EpistemicFormula.fromLiteral(applied);
    }

    @Override
    public int hashCode() {
        return getOriginalWrappedLiteral().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EpistemicFormula)) return false;
        EpistemicFormula formula = (EpistemicFormula) o;
        return Objects.equals(getOriginalWrappedLiteral(), formula.getOriginalWrappedLiteral());
    }

    @Override
    public String toString() {
        return getCleanedOriginal().toString();
    }
}