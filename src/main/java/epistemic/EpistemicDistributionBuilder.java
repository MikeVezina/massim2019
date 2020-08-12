package epistemic;

import epistemic.agent.EpistemicAgent;
import epistemic.wrappers.WrappedLiteral;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import org.apache.commons.lang.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import utils.Stopwatch;

import java.util.*;
import java.util.function.Function;

public class EpistemicDistributionBuilder {

    public static final Atom KB = ASSyntax.createAtom("kb");
    public static final Atom PROP_ANNOT = ASSyntax.createAtom("prop");
    public static final String IS_VALID_FUNCTOR = "is_valid";
    private EpistemicAgent epistemicAgent;
    private List<Literal> isValidLiterals;

    /**
     * Should be called once the agent has been loaded.
     * This method needs to process the belief base as a whole
     * after the initial agent has been loaded and processed.
     * This is required in order for logicalConsequences to work
     * correctly.
     *
     * @param agent Necessary for accessing BB rules and agent
     *              logical consequences for evaluation and expansion.
     */
    @NotNull
    public EpistemicDistribution createDistribution(@NotNull EpistemicAgent agent) {
        this.epistemicAgent = agent;
        this.isValidLiterals = new ArrayList<>();

        var managedWorlds = processDistribution();
//        System.out.println(managedWorlds.toString());

        return new EpistemicDistribution(this.epistemicAgent, managedWorlds);
    }


    /**
     * Process the distribution of worlds, create, and set the ManagedWorlds object.
     */
    protected ManagedWorlds processDistribution() {
        // Gets and processes all literals in the kb belief base that are marked with 'prop'
        var filteredLiterals = processLiterals(this::nsFilter, this::propFilter);

        // Generate the map of literal enumerations
        var literalMap = generateLiteralEnumerations(filteredLiterals);

        // Calculate total number of worlds to generate
        long worldCount = 1;
        for(var list : literalMap.values())
            worldCount *= list.isEmpty() ? 1 : list.size();


        // Create the distribution of worlds
         return generateWorlds(literalMap);
    }

    private Boolean nsFilter(Literal literal) {
        return literal.getNS().equals(KB);
    }


    /**
     * Adds literals to propLiterals marked with the [prop] annotation. Does nothing otherwise.
     *
     * @param literal The literal
     */
    private boolean propFilter(Literal literal) {
        return literal.hasAnnot(PROP_ANNOT);
    }

    /**
     * Iterates through the belief base, filters the beliefs, and returns the filtered literals/beliefs. Calls {@link EpistemicDistributionBuilder#processLiterals(Iterable, Function[])}.
     * If any of the filters return false for a given belief, it will not be returned. Filters are called in the order
     * that they are passed in.
     *
     * @return A list of filtered literals
     */
    @SafeVarargs
    protected final List<Literal> processLiterals(Function<Literal, Boolean>... filters) {
        return this.processLiterals(epistemicAgent.getBB(), filters);
    }

    /**
     * Iterates through the belief base (kb namespace only), filters them, and returns the filtered literals/beliefs.
     * If any of the filters return false for a given belief, it will not be returned. Filters are called in the order
     * that they are passed in.
     *
     * @return A list of filtered literals
     */
    @SafeVarargs
    protected final List<Literal> processLiterals(Iterable<Literal> literals, Function<Literal, Boolean>... filters) {
        // We need to iterate all beliefs.
        // We can't use beliefBase.getCandidateBeliefs(...) [aka the pattern matching function] because
        // the pattern matching function doesn't allow us to pattern match by just namespace and annotation
        // (it requires a functor and arity)
        List<Literal> filteredLiterals = new ArrayList<>();

        // Iterate through the belief base and call the consumers
        for (Literal belief : literals) {
            if (belief == null || !belief.getNS().equals(KB))
                continue;

            if (belief.getFunctor().equals(IS_VALID_FUNCTOR))
                isValidLiterals.add(belief);

            // Process belief through all filters
            var allFilterMatch = Arrays.stream(filters).allMatch((filter) -> filter.apply(belief));

            if (allFilterMatch)
                filteredLiterals.add(belief);

        }

        return filteredLiterals;
    }


    /**
     * Expands a rule (with variables) into a list of grounded literals. This essentially provides an enumeration of all variables values.
     * This maintains the functor and arity of the rule head, replacing variables with a value.
     * <p>
     * For example, given the beliefs: [test("abc"), test("123")],
     * the rule original_rule(Test) :- test(Test) will be expanded to the following grounded literals:
     * [original_rule("abc"), original_rule("123")]
     *
     * @param rule The rule to expand.
     * @return A List of ground literals.
     */
    protected HashSet<Literal> expandRule(Rule rule) {
        // Obtain the head and body of the rule
        Literal ruleHead = rule.getHead();
        LogicalFormula ruleBody = rule.getBody();

        // Get all unifications for the rule body
        Iterator<Unifier> unifIterator = ruleBody.logicalConsequence(this.epistemicAgent, new Unifier());

        // Set up a list of expanded literals
        HashSet<Literal> expandedLiterals = new HashSet<>();

        // Unify each valid unification with the plan head and add it to the belief base.
        while (unifIterator.hasNext()) {
            Unifier unif = unifIterator.next();

            // Clone and apply the unification to the rule head
            Literal expandedRule = (Literal) ruleHead.capply(unif);
            System.out.println("Unifying " + rule.getFunctor() + " with " + unif + ". Result: " + expandedRule);

            // All unified/expanded rules should be ground.
            if (!expandedRule.isGround()) {
                System.out.println("The expanded rule (" + expandedRule + ") is not ground.");
                for (int i = 0; i < expandedRule.getArity(); i++) {
                    Term t = expandedRule.getTerm(i);
                    if (!t.isGround())
                        System.out.println("Term " + t + " is not ground.");
                }
            }

            expandedLiterals.add(expandedRule);
        }

        return expandedLiterals;
    }

    /**
     * Generates a mapping of possible enumerations for each literal in allLiterals.
     * Right now this only supports rules (as it expands them into their possible values)
     *
     * @param propLiterals The list of literals (rules and beliefs) marked with [prop]
     * @return A Mapping of original literal to a list of possible enumerations.
     */
    protected LinkedHashMap<WrappedLiteral, Set<Literal>> generateLiteralEnumerations(List<Literal> propLiterals) {
        LinkedHashMap<WrappedLiteral, Set<Literal>> literalMap = new LinkedHashMap<>();

        for (Literal lit : propLiterals) {
            // Right now, we are only handling rules, but we can eventually extend support for beliefs
            if (lit.isRule()) {
                // Expand the rule into possible enumerations
                HashSet<Literal> expandedLiterals = expandRule((Rule) lit);

                // Put the enumerations into the mapping, with the original rule as the key
                var wrappedKey = new WrappedLiteral(lit);
                var prevValues = literalMap.put(wrappedKey, expandedLiterals);

                if (prevValues != null) {
                    epistemicAgent.getLogger().warning("There is an enumeration collision for the key: " + wrappedKey.getCleanedLiteral());
                    epistemicAgent.getLogger().warning("The following enumeration values have been overwritten: " + prevValues);
                }
            }
        }

        return literalMap;
    }


    /**
     * Generate worlds given a mapping of all propositions. This essentially generates all permutations of each of the possible enumeration values.
     *
     * @param allPropositionsMap This is a mapping of all literals (which are used to create the propositions used in each of the worlds)
     * @return A List of Valid worlds
     */
    protected ManagedWorlds generateWorlds(LinkedHashMap<WrappedLiteral, Set<Literal>> allPropositionsMap) {

        // Calculate total number of worlds to generate
        long worldCount = 1;
        for(var list : allPropositionsMap.values())
            worldCount *= list.isEmpty() ? 1 : list.size();


        System.out.println("Generating " + worldCount + " worlds");
        Stopwatch generationWatch = Stopwatch.startTiming();

        // Use backtracking to generate all worlds.
        Set<World> allWorlds = backtrackAllPropositions(new World(), allPropositionsMap);

        long msTime = generationWatch.stopMS();
        System.out.println("Generated " + allWorlds.size() + " worlds in " + msTime + " ms");

        Stopwatch filterWatch = Stopwatch.startTiming();
        // Only keep the worlds that are valid.
        try {
            return allWorlds.stream().filter(this::filterValidWorlds).collect(ManagedWorlds.WorldCollector(epistemicAgent));
        } finally {
            long filterMs = filterWatch.stopMS();
            System.out.println("World filtering took " + filterMs + "ms");
        }
    }

    /**
     * Generates all possible worlds from each proposition mapping. A LinkedHashMap is required here for the propositions as it allows
     * us to guarantee that the ordering of the keys will be consistent and predictable when iterating through
     * the map within each call of this function.
     *
     * This function takes in a world, finds the next available proposition, clones the world for each possible proposition value and recurses on
     * the cloned worlds. When there are no more available propositions, this means that the passed-in world is fully built and does not require any
     * further building. We then return all fully-built worlds.
     *
     * @param currentWorld the current world being built.
     * @param allPropositionsMap
     * @return
     */
    private Set<World> backtrackAllPropositions(World currentWorld, LinkedHashMap<WrappedLiteral, Set<Literal>> allPropositionsMap)
    {
        Set<World> allWorlds = new HashSet<>();

        WrappedLiteral nextAvailableKey = null;

        // Finds the first proposition key that doesn't exist in the world
        for(var propKey : allPropositionsMap.keySet())
        {
            if(!currentWorld.containsKey(propKey))
            {
                nextAvailableKey = propKey;
                break;
            }
        }

        // No more keys to iterate
        if(nextAvailableKey == null)
            return allWorlds;

        for(Literal val : allPropositionsMap.get(nextAvailableKey))
        {
            World cloned = currentWorld.clone();
            cloned.putLiteral(nextAvailableKey,val);

            // Iterate next literal key
            var result = backtrackAllPropositions(cloned,allPropositionsMap);

            // Return the result if the set is not empty
            if (!result.isEmpty())
                allWorlds.addAll(result);
            else
                // otherwise add our cloned world to the set.
                allWorlds.add(cloned);

        }

        return allWorlds;
    }

    /**
     * Uses the is_valid beliefs/rules to determine if a world is valid.
     * This essentially injects any world proposition values into the rule's terms.
     *
     * @param nextWorld The world to check.
     * @return True if the world is valid, false otherwise.
     */
    protected boolean filterValidWorlds(World nextWorld) {
        // If no rule is found, all worlds are valid.
        if (isValidLiterals == null || isValidLiterals.isEmpty())
            return true;

        // Iterate all 'isValid' literals
        for (Literal isValidLiteral : isValidLiterals) {

            var unifier = unifyValidWorldTerms(isValidLiteral, nextWorld);

            if(unifier == null)
                continue;

            // We apply the values in the unifier to the rule.
            var isValidUnified = (Literal) isValidLiteral.capply(unifier);

            // If there are any un-ground terms in the Literal, that means the world does not satisfy the term variables and is
            // therefore not suitable for evaluating the current world.
            if (!isValidUnified.isRule()) {
                if (isValidUnified.isGround())
                    // Handle ~is_Valid belief literal
                    return !isValidUnified.negated();
                continue;
            }

            Rule isValidRule = (Rule) isValidUnified;

            if (!isValidRule.getHead().isGround() || !isValidRule.getBody().isGround())
                continue;

            // The unified rule is executed to check if the world is valid. If hasNext returns true, then the rule was executed correctly.
            if (isValidRule.logicalConsequence(epistemicAgent, unifier).hasNext())
                // Handle ~is_valid rules
                return !isValidRule.negated();
        }

        // If no rules successfully evaluate with the nextWorld, it should not be filtered out.
        return true;
    }

    private Unifier unifyValidWorldTerms(Literal isValidRule, World nextWorld) {
        // Create a unifier
        Unifier unifier = new Unifier();

        // For each of the terms in the rule (i.e. one term would be 'kb::hand("Alice", Hand)'),
        // we want to see if one of the propositions in the world can unify any variables in that term (i.e. Hand).
        // If so, that variable is unified. We continue until all terms are unified. The unified values
        // are stored in the unifier object.
        for (Term t : isValidRule.getTerms()) {
            if (!t.isLiteral())
                continue;

            WrappedLiteral wrappedTerm = new WrappedLiteral((Literal) t).getNormalizedWrappedLiteral();
            Unifier termUnification = null;

            for (var lit : nextWorld.valueSet()) {
                // Unify the rule terms until we find a valid unification
                termUnification = wrappedTerm.unifyWrappedLiterals(lit.getValue());
                if (termUnification != null) {
                    unifier.compose(termUnification);
                    break;
                }
            }

            // If term unifier is null after iterating all world values,
            // we fail to unify the isValid term
            if(termUnification == null)
                return null;
        }

        return unifier;
    }


}
