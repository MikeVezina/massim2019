package epistemic.agent;

import epistemic.EpistemicDistribution;
import epistemic.EpistemicDistributionBuilder;
import epistemic.formula.EpistemicFormula;
import epistemic.Proposition;
import jason.JasonException;
import jason.RevisionFailedException;
import jason.asSemantics.Agent;
import jason.asSemantics.Intention;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import jason.bb.BeliefBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.*;

public class EpistemicAgent extends Agent {

    private EpistemicDistribution epistemicDistribution;
    private final EpistemicDistributionBuilder distributionBuilder;

    public EpistemicAgent() {
        this(new EpistemicDistributionBuilder());
    }

    public EpistemicAgent(@NotNull EpistemicDistributionBuilder distributionBuilder) {
        super.pl = new EpistemicPlanLibrary(new PlanLibrary());
        this.distributionBuilder = distributionBuilder;

    }

    @Override
    public void load(String asSrc) throws JasonException {
        super.load(asSrc);
        this.agentLoaded();
    }

    @Override
    public boolean addBel(Literal bel) throws RevisionFailedException {
        return super.addBel(bel);
    }

    @Override
    public void load(InputStream in, String sourceId) throws JasonException {
        super.load(in, sourceId);
        this.agentLoaded();
    }

    /**
     * Called when the agent has been loaded and parsed from the source file.
     * The possible worlds distribution can be initialized here. (Once initial beliefs/rules are parsed)
     */
    protected void agentLoaded() {
        System.out.println("Loaded");

        // Create the distribution after loading the agent successfully
        this.epistemicDistribution = distributionBuilder.createDistribution(this);

        // This will wrap the BB with a chained epistemic BB now that the distribution is set.
        this.setBB(this.getBB());

        // Call the distribution agent loaded function
        epistemicDistribution.agentLoaded();
    }

    @Override
    public void setPL(PlanLibrary pl) {
        // Ensure we always have a proxy plan library in place
        if(!(pl instanceof EpistemicPlanLibrary))
            pl = new EpistemicPlanLibrary(pl);

        super.setPL(pl);
    }


    /**
     * @return The current EpistemicDistribution. This will return null before {@link EpistemicAgent#agentLoaded()} is called.
     */
    @Nullable
    public EpistemicDistribution getEpistemicDistribution() {
        return epistemicDistribution;
    }

    @Override
    public EpistemicPlanLibrary getPL() {
        return (EpistemicPlanLibrary) super.getPL();
    }

    /**
     * Update managed world props in this function. We can still operate on beliefs.
     * Use super.addBel(), etc. to trigger the BRF (and generate events) for inferred knowledge.
     */
    @Override
    public int buf(Collection<Literal> percepts) {
        // Update the BB with new percepts before updating the managed worlds
        int result = super.buf(percepts);

        if (this.epistemicDistribution != null)
            this.epistemicDistribution.buf(percepts, this.getPL().getSubscribedFormulas());

        return result;
    }

    /**
     * Controls the addition / deletion of a belief. This function gets executed when +belief, -belief, or -+belief is executed by the ExecInt function.
     * <p>
     * We need to confirm that the model is correct. i.e. all proposition keys should have one (and only one) corresponding value in the BB.
     *
     * @see Agent#brf(Literal, Literal, Intention)
     */
    @Override
    public List<Literal>[] brf(Literal beliefToAdd, Literal beliefToDel, Intention i) throws RevisionFailedException {
        var revisionResult = new RevisionResult();

        if (this.epistemicDistribution != null)
            revisionResult.addResult(this.epistemicDistribution.brf(beliefToAdd, beliefToDel));
        else
            return super.brf(beliefToAdd, beliefToDel, i);

        // Forward any revisions to the super class to add to / delete from the BB
        var superRevision = new RevisionResult();

        // Add all revised propositions to the BB and keep track of any further revisions
        for(var additions : revisionResult.getAdditions())
        {
            superRevision.addResult(super.brf(additions, null, i));
        }

        // Add all revised propositions to the BB and keep track of any further revisions
        for(var deletion : revisionResult.getDeletions())
        {
            superRevision.addResult(super.brf(null, deletion, i));
        }

        return superRevision.buildResult();
    }

    public void setBB(BeliefBase beliefBase)
    {
        if(this.epistemicDistribution != null && !(beliefBase instanceof ChainedEpistemicBB))
            beliefBase = new ChainedEpistemicBB(beliefBase, this, this.epistemicDistribution);

        super.setBB(beliefBase);
    }


    /**
     * Unifies all possible values to an ungrounded epistemic formula
     * (i.e. an epistemic formula with an ungrounded root literal)
     *
     * @param epistemicFormula The epistemic formula with an ungrounded root literal.
     * @return The set of all grounded literals that unify with epistemicFormula.
     */
    public Set<EpistemicFormula> getCandidateFormulas(@NotNull EpistemicFormula epistemicFormula) {
        Set<EpistemicFormula> groundFormulaSet = new HashSet<>();


        // If the root literal is already ground, return a set containing the ground formula.
        if(epistemicFormula.getRootLiteral().getCleanedLiteral().isGround())
        {
            groundFormulaSet.add(epistemicFormula);
            return groundFormulaSet;
        }

        // Obtain all possible proposition values from the managed literals object and attempt to unify each value with the ungrounded epistemic formula.
        // All formulas that can be successfully ground will be added to the set.

        // TODO: This has issues. finding by predicate indicator does not incorporate negation in the way that we'd like (i.e. ignore it)
        for(Proposition managedValue : epistemicDistribution.getManagedBeliefs(epistemicFormula.getRootLiteral().getPredicateIndicator()))
        {
            // Create a cloned/normalized & ungrounded root literal to unify with
            var ungroundedLiteral = epistemicFormula.getRootLiteral().getNormalizedWrappedLiteral();
            var managedLiteral = managedValue.getValue().getNormalizedWrappedLiteral();

            // Attempt to unify with the various managed propositions
            Unifier unifier = ungroundedLiteral.unifyWrappedLiterals(managedLiteral);

            if(unifier != null)
            {
                var unifiedFormula = epistemicFormula.capply(unifier);

                if(!unifiedFormula.getCleanedOriginal().isGround()) {
                    getLogger().warning("formula " + epistemicFormula +" is still not ground after unifying: " + unifiedFormula);
                    continue;
                }

                groundFormulaSet.add(unifiedFormula);
            }
        }

        return groundFormulaSet;
    }
}
