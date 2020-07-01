package epistemic;

import epistemic.agent.RevisionResult;
import epistemic.reasoner.ReasonerSDK;
import epistemic.agent.EpistemicAgent;
import epistemic.formula.EpistemicFormula;
import epistemic.wrappers.NormalizedWrappedLiteral;
import jason.RevisionFailedException;
import jason.asSemantics.Event;
import jason.asSemantics.Intention;
import jason.asSyntax.*;
import epistemic.wrappers.WrappedLiteral;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * This class is responsible for being an interface between the Jason objects (TS, Agent, BeliefBase, etc.) and the managed worlds.
 */
public class EpistemicDistribution {

    private final Map<WrappedLiteral, Set<WrappedLiteral>> currentPropValues;
    private final Map<EpistemicFormula, Boolean> currentFormulaEvaluations;
    private final AtomicBoolean needsUpdate;
    private final ManagedWorlds managedWorlds;
    private final ReasonerSDK reasonerSDK;
    private final EpistemicAgent epistemicAgent;

    public EpistemicDistribution(@NotNull EpistemicAgent agent, @NotNull ManagedWorlds managedWorlds) {
        this(agent, managedWorlds, new ReasonerSDK());

    }

    public EpistemicDistribution(@NotNull EpistemicAgent agent, @NotNull ManagedWorlds managedWorlds, @NotNull ReasonerSDK reasonerSDK) {
        needsUpdate = new AtomicBoolean(false);
        this.currentPropValues = new HashMap<>();
        this.currentFormulaEvaluations = new HashMap<>();
        this.epistemicAgent = agent;
        this.managedWorlds = managedWorlds;

        // Create new reasonerSDK object that listens to the managed world events
        // This sends any necessary API requests
        this.reasonerSDK = reasonerSDK;

    }

    /**
     * Should be called once the agent has been loaded.
     */
    public void agentLoaded() {
        // Create the managed worlds
        reasonerSDK.createModel(managedWorlds);
    }

    /**
     * Gets called by the EpistemicAgent during the belief update phase. We use this phase of the reasoning cycle to
     * send proposition updates to the server and generate any relevant knowledge events.
     *
     * @param epistemicFormulas The formulas that will be evaluated.
     *                          Events will be created for the relevant formula evaluations.
     */
    public void buf(Collection<Literal> percepts, Collection<EpistemicFormula> epistemicFormulas) {

        // Pass percepts through this.BRF
        for (Literal literal : percepts) {
            try {
                epistemicAgent.brf(literal, null, Intention.EmptyInt);
            } catch (RevisionFailedException e) {
                epistemicAgent.getLogger().warning("EpistemicAgent brf failed when revising percepts: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }

        // No need to update props
        if (!this.shouldUpdateReasoner())
            return;

        // Ground all epistemic formulas before evaluating
        Set<EpistemicFormula> groundedFormulas = new HashSet<>();
        for (EpistemicFormula epistemicFormula : epistemicFormulas) {
            groundedFormulas.addAll(epistemicAgent.getCandidateFormulas(epistemicFormula));
        }

        Set<WrappedLiteral> propositionValues = new HashSet<>();
        for (var value : this.currentPropValues.values()) {
            propositionValues.addAll(value);
        }

        var knowledgeEntries = this.reasonerSDK.updateProps(propositionValues, groundedFormulas).entrySet();

        for (var knowledgePropEntry : knowledgeEntries) {
            var formula = knowledgePropEntry.getKey();
            var valuation = knowledgePropEntry.getValue();

            // Insert the valuation if the formula does not exist in the map
            var previousValuation = currentFormulaEvaluations.put(formula, valuation);

            // If the valuation is false and we did not have a previous valuation,
            // or if the valuation has not changed since the previous update
            // then we do not create any knowledge updates
            if ((!valuation && previousValuation == null) || valuation.equals(previousValuation))
                continue;

            // The event operator will be 'add' if the formula evaluates to true and was previously a false/null value.
            // The event operator will be 'del' if the formula evaluates to false and was previously true
            var eventOperator = valuation ? Trigger.TEOperator.add : Trigger.TEOperator.del;

            createKnowledgeEvent(eventOperator, formula);
        }

        this.needsUpdate.set(false);
    }

    protected Map<WrappedLiteral, Set<WrappedLiteral>> getCurrentPropValues() {
        return currentPropValues;
    }

    public RevisionResult brf(Literal beliefToAdd, Literal beliefToDel) {
        var revisions = new RevisionResult();

        Proposition addProp = managedWorlds.getManagedProposition(beliefToAdd);
        Proposition delProp = managedWorlds.getManagedProposition(beliefToDel);

        // Place the belief in the additions revisions since it isn't managed by us and should be passed directly to the BB
        if (addProp == null && beliefToAdd != null)
            revisions.addAddition(beliefToAdd);

        // Place the belief in the deletions revisions since it isn't managed by us and should be passed directly to the BB
        if (delProp == null && beliefToDel != null)
            revisions.addDeletion(beliefToDel);

        // If both add/del are not managed (or they contained null), then we do not need
        // to process the props further.
        if (addProp != null)
            revisions.addResult(addManagedBelief(addProp, beliefToAdd));


        if (delProp != null) {
            WrappedLiteral delWrapped = new WrappedLiteral(beliefToDel);

            if (isExistingProp(delProp.getKey(), delWrapped)) {
                // Remove wrapped deletion from current prop set for key
                this.currentPropValues.get(delProp.getKey()).remove(delWrapped);
                revisions.addDeletion(delWrapped.getOriginalLiteral());
            }
        }
        return revisions;

    }

    /**
     * Creates events for the relevant formula plans (+knows(knows(hello)))
     *
     * @param newKnowledge The new knowledge formula
     */
    protected void createKnowledgeEvent(Trigger.TEOperator operator, EpistemicFormula newKnowledge) {
        Trigger te = new Trigger(operator, Trigger.TEType.belief, newKnowledge.getCleanedOriginal());
        epistemicAgent.getTS().updateEvents(new Event(te, Intention.EmptyInt));
    }

    /**
     * @return True if the reasoner needs to be updated, false otherwise.
     */
    protected boolean shouldUpdateReasoner() {
        return this.needsUpdate.get();
    }

    /**
     * Adds a new belief to the map of current proposition values.
     * We also need to do additional consistency checks to make sure there are no contradicting proposition values.
     *
     * @param beliefToAdd
     */
    RevisionResult addManagedBelief(Proposition managedProp, Literal beliefToAdd) {
        var revisions = new RevisionResult();

        if (beliefToAdd == null) {
            // Add belief to revision if it is not managed by us
            revisions.addAddition(beliefToAdd);
            return revisions;
        }

        WrappedLiteral addWrapped = new WrappedLiteral(beliefToAdd);

        if (!isExistingProp(managedProp.getKey(), addWrapped)) {

            this.currentPropValues.compute(managedProp.getKey(), (key, val) -> {
                if (val == null)
                    val = new HashSet<>();

                // Merge new revisions into the revisions list
                revisions.addResult(forceConsistentAdd(val, addWrapped));

                return val;
            });

            this.needsUpdate.set(true);
        }

        return revisions;
    }

    /**
     * Forces Proposition consistency so that adding a new proposition does not contradict current propositions.
     * This returns the revision results (additions and removals) necessary to perform the addition. This modifies the
     * curProps set to accommodate the added newProp.
     * <br>
     * Here are a few input/output examples: <br/>
     * <strong>Example 1: New Prop is negated and replaces current non-negated equivalent.</strong>
     * <div>
     * curProps = { "hand", "other" } <br/>
     * newProp = { "~hand" } <br/>
     * <br><strong>Output:</strong><br/>
     * addRevision = { "~hand" } <br/>
     * delRevision = { "hand" } <br/>
     * curProps = { "~hand", "other" }
     * </div>
     * <br/>
     * <strong>Example 2: New Prop is positive and replaces all negated current props (this is because a positive proposition implies negation of all others in the current prop set).</strong>
     * <div>
     * curProps = { "~hand('AA')", "~hand('A8')" } <br/>
     * newProp = { "hand('AA')" } <br/>
     * <br/>
     * <strong>Output:</strong><br/>
     * addRevision = { "hand('AA')" } <br/>
     * delRevision = { "~hand('AA')", "~hand('A8')" } <br/>
     * curProps = { "hand('AA')" }
     * </div>
     *
     * @param curProps The current proposition set. This will be modified to reflect the proposition revisions.
     * @param newProp  The new proposition being added
     * @return The necessary revisions to accommodate the addition. This includes the newProp in the additions.
     */
    private RevisionResult forceConsistentAdd(Set<WrappedLiteral> curProps, WrappedLiteral newProp) {
        var revisions = new RevisionResult();

        if (curProps.isEmpty() || newProp.isNormalized()) {

            // We want to add any existing normalized (non-negated) beliefs to the removed list
            revisions.addAllDeletions(curProps.stream().map(WrappedLiteral::getOriginalLiteral).collect(Collectors.toList()));
            // Remove all existing proposition values since there can only be one positive enumeration value at a time
            curProps.clear();

            curProps.add(newProp);
            revisions.addAddition(newProp.getOriginalLiteral());
            return revisions;
        }

        // Check if there are any existing normalized propositions
        // If so, we shouldn't change the list because adding a negated prop will be redundant.
        // For example: adding ~hand('Alice', 'AA') is redundant when we already know hand('Alice', 'A8')
        // and that alone implies ~hand('Alice', 'AA') and ~hand('Alice', '88').

        WrappedLiteral normalizedProp = null;
        for (WrappedLiteral curProp : curProps) {
            if (curProp.isNormalized()) {
                normalizedProp = curProp;
                break;
            }
        }


        // However, if we have the current prop hand('Alice', 'AA') and our new prop is
        // ~hand('Alice', 'AA'), then we need to overwrite the normalized prop with the negated newProp.
        // This occurs when we no longer know something, i.e. alice has AA.
        // We do this by converting the negated newProp into a normalized prop and checking for equivalency.
        if (normalizedProp != null) {
            // The new prop should not overwrite the old normalized prop
            if (!normalizedProp.getNormalizedWrappedLiteral().equals(newProp.getNormalizedWrappedLiteral()))
                return revisions;

            // Remove the old prop and continue
            curProps.remove(normalizedProp);

            revisions.addDeletion(normalizedProp.getOriginalLiteral());
        }

        curProps.add(newProp);
        revisions.addAddition(newProp.getOriginalLiteral());

        return revisions;
    }

    public ManagedWorlds getManagedWorlds() {
        return this.managedWorlds;
    }

    public Map<EpistemicFormula, Boolean> evaluateFormulas(Set<EpistemicFormula> epistemicFormula) {
        return this.reasonerSDK.evaluateFormulas(epistemicFormula);
    }


    /**
     * Determines if the key already maps to the newValue.
     *
     * @param key      The key to check the existing value for
     * @param newValue The new value of the key
     * @return True if the new value is equivalent to the old value, false otherwise.
     */
    private boolean isExistingProp(WrappedLiteral key, WrappedLiteral newValue) {
        if (key == null || !this.currentPropValues.containsKey(key))
            return false;

        var curPropSet = this.currentPropValues.get(key);
        return curPropSet.contains(newValue);


    }

    public EpistemicAgent getEpistemicAgent() {
        return this.epistemicAgent;
    }

    public Set<Proposition> getManagedBeliefs(PredicateIndicator predicateIndicator) {
        return getManagedWorlds().getManagedLiterals().getManagedBeliefs(predicateIndicator);
    }
}
