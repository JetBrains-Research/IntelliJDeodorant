package gr.uom.java.ast;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceExpression;

public class LocalVariableInstructionObject {
    private TypeObject type;
    private String name;
    private ASTInformation simpleName;
    private volatile int hashCode = 0;
    private String variableBindingKey;

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

    public String getVariableBindingKey() {
        return variableBindingKey;
    }

    public void setSimpleName(PsiReferenceExpression simpleName) {
        this.variableBindingKey = simpleName.getReferenceName();
        this.simpleName = ASTInformationGenerator.generateASTInformation(simpleName);
    }

    public PsiReferenceExpression getSimpleName() {
        PsiElement node = this.simpleName.recoverASTNode();
        return (PsiReferenceExpression) node;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof LocalVariableInstructionObject) {
            LocalVariableInstructionObject lvio = (LocalVariableInstructionObject) o;
            return this.name.equals(lvio.name) && this.type.equals(lvio.type) &&
                    this.variableBindingKey.equals(lvio.variableBindingKey);
        }
        return false;
    }

    public boolean equals(LocalVariableDeclarationObject lvdo) {
        return this.name.equals(lvdo.getName()) && this.type.equals(lvdo.getType()) && this.variableBindingKey.equals(lvdo.getVariableBindingKey());
    }

    public boolean equals(ParameterObject parameter) {
        return this.name.equals(parameter.getName()) && this.type.equals(parameter.getType()) && this.variableBindingKey.equals(parameter.getVariableBindingKey());
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + type.hashCode();
            result = 37 * result + name.hashCode();
            result = 37 * result + variableBindingKey.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(" ");
        sb.append(name);
        return sb.toString();
    }
}
