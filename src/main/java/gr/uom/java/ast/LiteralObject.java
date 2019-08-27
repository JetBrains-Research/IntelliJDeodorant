package gr.uom.java.ast;

import com.intellij.psi.*;

import java.util.Objects;

public class LiteralObject {
    private LiteralType literalType;
    private String value;
    private TypeObject type;
    private PsiExpression literal;
    private volatile int hashCode = 0;

    public LiteralObject(PsiExpression expression) {
        if (expression instanceof PsiLiteralExpression) {
            PsiElement psiElement = expression.getFirstChild();
            if (Objects.equals(psiElement.getText(), PsiKeyword.NULL)) {
                literalType = LiteralType.NULL;
                value = "null";
                type = TypeObject.extractTypeObject(expression.getText());
            }
            if (Objects.equals(psiElement.getText(), PsiKeyword.TRUE) || Objects.equals(psiElement.getText(), PsiKeyword.FALSE)) {
                literalType = LiteralType.BOOLEAN;
                value = expression.getText();
                type = TypeObject.extractTypeObject(expression.getText());
            }
            if (psiElement instanceof PsiTypeElement) {
                literalType = LiteralType.CHARACTER;
                value = psiElement.getText();
                type = TypeObject.extractTypeObject(expression.getText());
            }
            
/*        } else if (expression instanceof NumberLiteral) {
            NumberLiteral numberLiteral = (NumberLiteral) expression;
            literalType = LiteralType.NUMBER;
            value = numberLiteral.getToken();
            type = TypeObject.extractTypeObject(numberLiteral.resolveTypeBinding().getQualifiedName());
        } else if (expression instanceof BooleanLiteral) {
            BooleanLiteral booleanLiteral = (BooleanLiteral) expression;
            literalType = LiteralType.BOOLEAN;
            value = Boolean.toString(booleanLiteral.booleanValue());
            type = TypeObject.extractTypeObject(booleanLiteral.resolveTypeBinding().getQualifiedName());
        } else if (expression instanceof CharacterLiteral) {
            CharacterLiteral characterLiteral = (CharacterLiteral) expression;
            literalType = LiteralType.CHARACTER;
            value = Character.toString(characterLiteral.charValue());
            type = TypeObject.extractTypeObject(characterLiteral.resolveTypeBinding().getQualifiedName());
        } else if (expression instanceof TypeLiteral) {
            TypeLiteral typeLiteral = (TypeLiteral) expression;
            literalType = LiteralType.TYPE;
            value = typeLiteral.getType().toString();
            type = TypeObject.extractTypeObject(typeLiteral.resolveTypeBinding().getQualifiedName());
        }
            this.literal = ASTInformationGenerator.generateASTInformation(expression);
        }*/
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
            expression = literal;
        } else if (literalType.equals(LiteralType.CHARACTER)) {
            expression =  literal;
/*        } else if (literalType.equals(LiteralType.NULL)) {
            expression = (NullLiteral) literal.recoverASTNode();
        } else if (literalType.equals(LiteralType.NUMBER)) {
            expression = (NumberLiteral) literal.recoverASTNode();
        } else if (literalType.equals(LiteralType.STRING)) {
            expression = (StringLiteral) literal.recoverASTNode();
        } else if (literalType.equals(LiteralType.TYPE)) {
            expression = (TypeLiteral) literal.recoverASTNode();
        }*/
        } else if (literalType.equals(LiteralType.NULL)) {
            expression =  literal;
        } else if (literalType.equals(LiteralType.NUMBER)) {
            expression = literal;
        } else if (literalType.equals(LiteralType.STRING)) {
            expression =  literal;
        } else if (literalType.equals(LiteralType.TYPE)) {
            expression =  literal;
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
