package core.ast;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceExpression;

public class LocalVariableInstructionObject {
    private final TypeObject type;
    private final String name;
    private ASTInformation reference;
    private volatile int hashCode = 0;

    public LocalVariableInstructionObject(TypeObject type, String name) {
        this.type = type;
        this.name = name;
    }

    public TypeObject getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setReference(PsiReferenceExpression reference) {
        this.reference = ASTInformationGenerator.generateASTInformation(reference);
    }

    public PsiReferenceExpression getReference() {
        PsiElement node = this.reference.recoverASTNode();
        return (PsiReferenceExpression) node;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof LocalVariableInstructionObject) {
            LocalVariableInstructionObject lvio = (LocalVariableInstructionObject) o;
            return this.name.equals(lvio.name) && this.type.equals(lvio.type) &&
                    this.reference.equals(lvio.reference);
        }
        return false;
    }

    public boolean equals(LocalVariableDeclarationObject lvdo) {
        return this.name.equals(lvdo.getName()) && this.type.equals(lvdo.getType());
    }

    public boolean equals(ParameterObject parameter) {
        return this.name.equals(parameter.getName())
                && this.type.equals(parameter.getType());
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + type.hashCode();
            result = 37 * result + name.hashCode();
            result = 37 * result + reference.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        return type + " " + name;
    }
}
