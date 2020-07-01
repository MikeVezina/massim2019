package epistemic.wrappers;

import jason.asSyntax.Literal;
import jason.asSyntax.LiteralImpl;

/**
 * A Wrapped that cleans any negation
 */
public class NormalizedWrappedLiteral extends WrappedLiteral {
    public NormalizedWrappedLiteral(Literal literal) {
        super(literal);
    }

    @Override
    protected Literal cleanLiteral(Literal originalLiteral) {
        var cleanWrapped = super.cleanLiteral(originalLiteral);

        if(cleanWrapped instanceof LiteralImpl)
            cleanWrapped = cleanWrapped.setNegated(Literal.LPos);

        return cleanWrapped;
    }

}
