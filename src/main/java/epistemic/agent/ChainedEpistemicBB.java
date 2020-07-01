package epistemic.agent;

import epistemic.EpistemicDistribution;
import epistemic.formula.EpistemicFormula;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.PredicateIndicator;
import jason.bb.BeliefBase;
import jason.bb.ChainBBAdapter;
import jason.bb.DefaultBeliefBase;

import java.util.ArrayList;
import java.util.Iterator;

public class ChainedEpistemicBB extends ChainBBAdapter {
    private final EpistemicDistribution epistemicDistribution;
    private final EpistemicAgent epistemicAgent;

    public ChainedEpistemicBB(BeliefBase beliefBase, EpistemicAgent agent, EpistemicDistribution distribution) {
        super(beliefBase != null ? beliefBase : new DefaultBeliefBase());
        this.epistemicDistribution = distribution;
        this.epistemicAgent = agent;
    }

    @Override
    public Iterator<Literal> getCandidateBeliefs(PredicateIndicator pi) {
        // The predicate indicator doesn't work with epistemic beliefs,
        // i.e: it will always be knows/1, we need the root literal to be able to evaluate anything
        // We just forward this to the actual belief base.
        return super.getCandidateBeliefs(pi);
    }

    @Override
    public Iterator<Literal> getCandidateBeliefs(Literal l, Unifier u) {
        // Copy & Apply the unifier to the literal
        Literal unifiedLiteral = (Literal) l.capply(u);

        var epistemicLiteral = EpistemicFormula.fromLiteral(unifiedLiteral);

        // Unified literal is not an epistemic literal.
        if (epistemicLiteral == null)
            return super.getCandidateBeliefs(l, u);

        // If the literal is not managed by us, we delegate to the chained BB.
        if (!epistemicDistribution.getManagedWorlds().getManagedLiterals().isManagedBelief(epistemicLiteral.getRootLiteral().getPredicateIndicator())) {
            epistemicAgent.getLogger().warning("The root literal in the epistemic formula: " + epistemicLiteral.getCleanedOriginal() + " is not managed by the reasoner. Delegating to BB.");
            return super.getCandidateBeliefs(l, u);
        }

        // If the root literal is not ground, then obtain all possible managed unifications
        var groundFormulas = epistemicAgent.getCandidateFormulas(epistemicLiteral);

        var result = epistemicDistribution.evaluateFormulas(groundFormulas);
        var arr = new ArrayList<Literal>();

        // If the result is true (formula evaluated to true), then return the literal as a candidate belief
        for(var formulaResultEntry : result.entrySet()) {

            // Add formula literal to results list if the formula was evaluated to true
            if(formulaResultEntry.getValue())
                arr.add(formulaResultEntry.getKey().getCleanedOriginal());
        }

        // Return null if no candidates
        // This maintains the original BB functionality
        if(arr.isEmpty())
            return null;

        return arr.iterator();
    }



}
