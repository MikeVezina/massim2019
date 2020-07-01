package epistemic.wrappers;

import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Map;

public class WrappedTerm extends DefaultTerm
{
    private final Term wrappedTerm;

    // We utilize a constant hash code for any var terms.
    private final int VAR_HASH = ASSyntax.createVar("Var").hashCode();

    public WrappedTerm(Term wrappedTerm)
    {
        this.wrappedTerm = wrappedTerm;

        if(!(wrappedTerm instanceof Literal))
            return;

        Literal termLiteral = (Literal) wrappedTerm;

        var arity = termLiteral.getArity();

        if(arity <= 0)
            return;

        for(int i = 0; i < arity; i++)
        {
            var curTerm = termLiteral.getTerm(i);

            // Do not wrap again
            if(curTerm instanceof WrappedTerm)
                continue;

            termLiteral.setTerm(i, new WrappedTerm(curTerm));
        }
    }

    public Term getOriginalTerm() {
        return wrappedTerm;
    }

    @Override
    public boolean isSet() {
        return wrappedTerm.isSet();
    }

    @Override
    public boolean isMap() {
        return wrappedTerm.isMap();
    }

    @Override
    public boolean isVar() {
        return wrappedTerm.isVar();
    }

    @Override
    public boolean isUnnamedVar() {
        return wrappedTerm.isUnnamedVar();
    }

    @Override
    public boolean isLiteral() {
        return wrappedTerm.isLiteral();
    }

    @Override
    public boolean isRule() {
        return wrappedTerm.isRule();
    }

    @Override
    public boolean isList() {
        return wrappedTerm.isList();
    }

    @Override
    public boolean isString() {
        return wrappedTerm.isString();
    }

    @Override
    public boolean isInternalAction() {
        return wrappedTerm.isInternalAction();
    }

    @Override
    public boolean isArithExpr() {
        return wrappedTerm.isArithExpr();
    }

    @Override
    public boolean isNumeric() {
        return wrappedTerm.isNumeric();
    }

    @Override
    public boolean isPred() {
        return wrappedTerm.isPred();
    }

    @Override
    public boolean isGround() {
        return wrappedTerm.isGround();
    }

    @Override
    public boolean isStructure() {
        return wrappedTerm.isStructure();
    }

    @Override
    public boolean isAtom() {
        return wrappedTerm.isAtom();
    }

    @Override
    public boolean isPlanBody() {
        return wrappedTerm.isPlanBody();
    }

    @Override
    public boolean isCyclicTerm() {
        return wrappedTerm.isCyclicTerm();
    }

    @Override
    public boolean hasVar(VarTerm varTerm, Unifier unifier) {
        return wrappedTerm.hasVar(varTerm, unifier);
    }

    @Override
    public VarTerm getCyclicVar() {
        return wrappedTerm.getCyclicVar();
    }

    @Override
    public void countVars(Map<VarTerm, Integer> map) {
        wrappedTerm.countVars(map);
    }

    @Override
    public Term clone() {
        return new WrappedTerm(wrappedTerm.clone());
    }

    @Override
    public boolean subsumes(Term term) {
        return wrappedTerm.subsumes(term);
    }

    @Override
    public Term capply(Unifier unifier) {
        return wrappedTerm.capply(unifier);
    }

    @Override
    public Term cloneNS(Atom atom) {
        return wrappedTerm.cloneNS(atom);
    }

    @Override
    public void setSrcInfo(SourceInfo sourceInfo) {
        wrappedTerm.setSrcInfo(sourceInfo);
    }

    @Override
    public SourceInfo getSrcInfo() {
        return wrappedTerm.getSrcInfo();
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Term))
            return false;

        Term term = (Term) o;

        // Unwrap the terms
        if(term instanceof WrappedTerm)
            term = ((WrappedTerm) term).wrappedTerm;

        return (term.isVar() && wrappedTerm.isVar() || wrappedTerm.equals(term));
    }

    @Override
    protected int calcHashCode() {
        if(wrappedTerm.isVar())
            return VAR_HASH;

        return wrappedTerm.hashCode();
    }

    @Override
    public Element getAsDOM(Document document) {
        return wrappedTerm.getAsDOM(document);
    }

    @Override
    public int compareTo(Term o) {
        return wrappedTerm.compareTo(o);
    }

    @Override
    public String toString() {
        return "WT(" +
                wrappedTerm +
                ')';
    }

    @Override
    public String getAsJSON(String indentation) {
        return wrappedTerm.getAsJSON(indentation);
    }
}
