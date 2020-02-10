package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import com.intellij.psi.tree.IElementType;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class LiteralObject {
    private LiteralType literalType;
    private String value;
    private TypeObject type;
    private SmartPsiElementPointer<PsiExpression> literal;
    private volatile int hashCode = 0;

    public LiteralObject(PsiExpression expression) {
        if (expression instanceof PsiLiteralExpression) {
            PsiLiteralExpressionImpl literalExpression = (PsiLiteralExpressionImpl) expression;
            IElementType literalExpressionType = literalExpression.getLiteralElementType();
            if (JavaTokenType.STRING_LITERAL.equals(literalExpressionType)) {
                literalType = LiteralType.STRING;
                value = literalExpression.getCanonicalText();
                type = TypeObject.extractTypeObject(literalExpressionType.toString());
            } else if (JavaTokenType.NULL_KEYWORD.equals(literalExpressionType)) {
                literalType = LiteralType.NULL;
                value = "null";
                type = TypeObject.extractTypeObject(literalExpressionType.toString());
            } else if (JavaTokenType.INTEGER_LITERAL.equals(literalExpressionType)) {
                literalType = LiteralType.NUMBER;
                value = literalExpression.getCanonicalText();
                type = TypeObject.extractTypeObject(literalExpressionType.toString());
            } else if (JavaTokenType.FLOAT_KEYWORD.equals(literalExpressionType)) {
                literalType = LiteralType.NUMBER;
                value = literalExpression.getCanonicalText();
                type = TypeObject.extractTypeObject(literalExpressionType.toString());
            } else if (JavaTokenType.DOUBLE_COLON.equals(literalExpressionType)) {
                literalType = LiteralType.NUMBER;
                value = literalExpression.getCanonicalText();
                type = TypeObject.extractTypeObject(literalExpressionType.toString());
            } else if (JavaTokenType.TRUE_KEYWORD.equals(literalExpressionType)) {
                literalType = LiteralType.BOOLEAN;
                value = literalExpression.getCanonicalText();
                type = TypeObject.extractTypeObject(literalExpressionType.toString());
            } else if (JavaTokenType.FALSE_KEYWORD.equals(literalExpressionType)) {
                literalType = LiteralType.BOOLEAN;
                value = literalExpression.getCanonicalText();
                type = TypeObject.extractTypeObject(literalExpressionType.toString());
            } else if (JavaTokenType.CHARACTER_LITERAL.equals(literalExpressionType)) {
                literalType = LiteralType.CHARACTER;
                value = literalExpression.getCanonicalText();
                type = TypeObject.extractTypeObject(literalExpressionType.toString());
            }
            this.literal = toPointer(expression);
        }
    }

    public LiteralType getLiteralType() {
        return literalType;
    }

    public String getValue() {
        return value;
    }

    public TypeObject getType() {
        return type;
    }

    public PsiExpression getLiteral() {
        PsiExpression expression = null;
        if (literalType.equals(LiteralType.BOOLEAN)) {
            expression = literal.getElement();
        } else if (literalType.equals(LiteralType.CHARACTER)) {
            expression = literal.getElement();
        } else if (literalType.equals(LiteralType.NULL)) {
            expression = literal.getElement();
        } else if (literalType.equals(LiteralType.NUMBER)) {
            expression = literal.getElement();
        } else if (literalType.equals(LiteralType.STRING)) {
            expression = literal.getElement();
        } else if (literalType.equals(LiteralType.TYPE)) {
            expression = literal.getElement();
        }
        return expression;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof LiteralObject) {
            LiteralObject literalObject = (LiteralObject) o;
            return this.literalType.equals(literalObject.literalType) && this.value.equals(literalObject.value);
        }

        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + value.hashCode();
            result = 37 * result + literalType.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        return value;
    }
}
