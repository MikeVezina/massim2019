package epistemic;

import epistemic.wrappers.NormalizedWrappedLiteral;
import epistemic.wrappers.WrappedLiteral;
import jason.asSyntax.Literal;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Objects;

/**
 * A Proposition is a mapping of a wrapped literal key and value in a given world.
 * The key corresponds to the literal that introduced the value (i.e. an expanded rule head).
 *
 * Both the key and value will be normalized literals since proposition literals can not be negated, contain custom namespaces, or contain annotations.
 */
public class Proposition extends AbstractMap.SimpleEntry<NormalizedWrappedLiteral, NormalizedWrappedLiteral> {

    public Proposition(@NotNull NormalizedWrappedLiteral literalKey, @NotNull NormalizedWrappedLiteral literalValue) {
        super(literalKey, literalValue);

        if (!this.getValue().isGround())
            throw new IllegalArgumentException("literalValue is not ground");

        if(!getValue().canUnify(getKey()))
            throw new IllegalArgumentException("The literalValue can not unify the literalKey. Failed to create Proposition.");
    }

    public Proposition(@NotNull WrappedLiteral literalKey, @NotNull Literal literalValue) {
        this(literalKey.getNormalizedWrappedLiteral(), new WrappedLiteral(literalValue).getNormalizedWrappedLiteral());
    }

    public Proposition(@NotNull WrappedLiteral literalKey, @NotNull WrappedLiteral literalValue) {
        this(literalKey.getNormalizedWrappedLiteral(), literalValue.getNormalizedWrappedLiteral());
    }

    @Override
    public NormalizedWrappedLiteral getKey() {
        return super.getKey();
    }

    @Override
    public NormalizedWrappedLiteral getValue() {
        return super.getValue();
    }

    public Literal getKeyLiteral() {
        return super.getKey().getCleanedLiteral();
    }

    public Literal getValueLiteral() {
        return super.getValue().getCleanedLiteral();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getValue());
    }
}


