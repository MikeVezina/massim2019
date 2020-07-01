package epistemic.agent;

import jason.asSyntax.Literal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A better way of managing the revision result returned by the BRF.
 */
public class RevisionResult {
    private List<Literal> additions;
    private List<Literal> deletions;

    @Override
    public String toString() {
        return "RevisionResult {" +
                "additions=" + additions +
                ", deletions=" + deletions +
                " }";
    }

    public RevisionResult() {
        this.additions = new ArrayList<>();
        this.deletions = new ArrayList<>();
    }

    public RevisionResult(List<Literal> additions, List<Literal> deletions) {
        this();
        addAllAdditions(additions);
        addAllDeletions(deletions);
    }

    public void addAllAdditions(@NotNull List<Literal> additions) {
        this.additions.addAll(additions);
    }

    public void addAllDeletions(@NotNull List<Literal> deletions) {
        this.deletions.addAll(deletions);
    }

    public void addResult(List<Literal>[] revisionsResult) {
        if (revisionsResult == null)
            return;

        addAllAdditions(revisionsResult[0]);
        addAllDeletions(revisionsResult[1]);
    }

    /**
     * @return An 2 element array of literal lists. The first element is the list
     * of literal additions and the second element is the list of literal deletions.
     * If nothing has changed, this will return null.
     */
    public List<Literal>[] buildResult() {
        if (additions.isEmpty() && deletions.isEmpty())
            return null;

        return new List[]{new ArrayList<>(additions), new ArrayList<>(additions)};
    }

    public void addResult(RevisionResult revisionResult) {
        if (revisionResult == null)
            return;

        addAllAdditions(revisionResult.additions);
        addAllDeletions(revisionResult.deletions);
    }


    public List<Literal> getAdditions() {
        return additions;
    }

    public List<Literal> getDeletions() {
        return deletions;
    }

    public void addAddition(Literal literal) {
        if (literal == null)
            return;

        this.additions.add(literal);
    }

    public void addDeletion(Literal literal) {
        if (literal == null)
            return;

        this.deletions.add(literal);
    }
}
