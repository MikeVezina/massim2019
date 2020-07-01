package epistemic.agent;

import epistemic.formula.EpistemicFormula;
import jason.JasonException;
import jason.asSyntax.*;
import jason.asSyntax.parser.ParseException;
import org.jetbrains.annotations.NotNull;

import java.util.*;


/**
 * A proxy for the plan library to hook any plans for knowledge events
 * so that we can subscribe to the appropriate formulas.
 *
 * For example, if the plan +know(know("Alice")) is added to the plan library,
 * we should subscribe to the formula "(k (k "Alice"))".
 *
 * The subscribed formulas can be obtained and sent to the reasoner
 * for evaluation and Agent event generation.
 *
 * The only methods that will be changed are the add/remove methods,
 * all other methods call the proxied library without any modifications.
 */
public class EpistemicPlanLibrary extends PlanLibrary {
    private final Map<Plan, EpistemicFormula> subscriptionPlans;

    public EpistemicPlanLibrary(PlanLibrary pl) {
        this();

        // If the proxied library already has plans, iterate through them to add any subscriptions.
        try {
            this.addAll(pl);
        } catch (JasonException e) {
            throw new IllegalArgumentException("Failed to add all plans to proxy plan library", e);
        }
    }

    public EpistemicPlanLibrary() {
        subscriptionPlans = new HashMap<>();
    }

    public List<EpistemicFormula> getSubscribedFormulas() {
        return new ArrayList<>(subscriptionPlans.values());
    }

    /**
     * Looks for event plans for epistemic formulas literal in the event trigger, creates
     * an EpistemicFormula object from the literal, and adds the formula to the subscribed formulas map.
     *
     * @param newPlan The plan that was added to the plan library.
     */
    private Plan addEpistemicPlan(@NotNull Plan newPlan) {
        // We should not subscribe to any achieve/goal plans
        if(!EpistemicFormula.isEpistemicLiteral(newPlan.getTrigger().getLiteral()) || !newPlan.getTrigger().getType().equals(Trigger.TEType.belief))
            return newPlan;

        subscriptionPlans.put(newPlan, EpistemicFormula.fromLiteral(newPlan.getTrigger().getLiteral()));
        return newPlan;
    }

    /**
     * Checks for knowledge formulas from the plan library.
     * @param planLibrary The plan library
     */
    private void addEpistemicPlan(@NotNull PlanLibrary planLibrary)
    {
        addEpistemicPlan(planLibrary.getPlans());
    }

    /**
     * Checks for knowledge formulas from the list of plans.
     * @param plans The list of plans
     */
    private void addEpistemicPlan(List<Plan> plans) {
        if(plans == null)
            return;

        for(Plan plan : plans)
            this.addEpistemicPlan(plan);
    }

    private Plan removeEpistemicPlan(@NotNull Plan removedPlan) {
        if(!EpistemicFormula.isEpistemicLiteral(removedPlan.getTrigger().getLiteral()))
            return removedPlan;

        subscriptionPlans.remove(removedPlan);
        return removedPlan;
    }

    @Override
    public Plan add(Plan p, Term source, boolean before) throws JasonException {
        return addEpistemicPlan(super.add(p, source, before));
    }

    @Override
    public Plan add(Plan p, boolean before) throws JasonException {
        return addEpistemicPlan(super.add(p, before));
    }

    @Override
    public Plan add(Plan p) throws JasonException {
        return addEpistemicPlan(super.add(p));
    }

    @Override
    public void addAll(PlanLibrary pl) throws JasonException {
        super.addAll(pl);
        addEpistemicPlan(pl);
    }

    @Override
    public void addAll(List<Plan> plans) throws JasonException {
        super.addAll(plans);
        addEpistemicPlan(plans);
    }

    @Override
    public void clear() {
        super.clear();
        subscriptionPlans.clear();
    }

    @Override
    public boolean remove(Literal pLabel, Term source) {
        var plan = get(pLabel);

        var removedPlan = super.remove(pLabel, source);

        if(removedPlan)
            removeEpistemicPlan(plan);

        return removedPlan;
    }

    @Override
    public Plan remove(Literal pLabel) {
        return removeEpistemicPlan(super.remove(pLabel));
    }

    @Override
    public EpistemicPlanLibrary clone() {
        var clone = new EpistemicPlanLibrary(super.clone());
        clone.subscriptionPlans.putAll(this.subscriptionPlans);
        return clone;
    }

}
