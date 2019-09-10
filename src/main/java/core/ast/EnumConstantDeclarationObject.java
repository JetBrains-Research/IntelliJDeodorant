package core.ast;

import com.intellij.psi.PsiDeclarationStatement;
import core.ast.decomposition.AbstractExpression;

import java.util.ArrayList;
import java.util.List;

class EnumConstantDeclarationObject {
    private final String name;
    private final List<AbstractExpression> arguments;
    private String enumName;
    private ASTInformation enumConstantDeclaration;
    private volatile int hashCode = 0;

    public EnumConstantDeclarationObject(String name) {
        this.name = name;
        this.arguments = new ArrayList<>();
    }

    public void setEnumConstantDeclaration(PsiDeclarationStatement enumConstantDeclaration) {
        this.enumConstantDeclaration = ASTInformationGenerator.generateASTInformation(enumConstantDeclaration);
    }

    public PsiDeclarationStatement getEnumConstantDeclaration() {
        return (PsiDeclarationStatement) this.enumConstantDeclaration.recoverASTNode();
    }

    public String getName() {
        return name;
    }

    public void addArgument(AbstractExpression expression) {
        this.arguments.add(expression);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof EnumConstantDeclarationObject) {
            EnumConstantDeclarationObject enumConstantDeclarationObjectObject = (EnumConstantDeclarationObject) o;
            return this.enumName.equals(enumConstantDeclarationObjectObject.enumName) &&
                    this.name.equals(enumConstantDeclarationObjectObject.name);
        }
        return false;
    }

    public void setEnumName(String enumName) {
        this.enumName = enumName;
    }

    public String getEnumName() {
        return this.enumName;
    }

    public boolean equals(FieldInstructionObject fio) {
        return this.enumName.equals(fio.getOwnerClass()) &&
                this.name.equals(fio.getName());
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + enumName.hashCode();
            result = 37 * result + name.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (!arguments.isEmpty()) {
            sb.append("(");
            for (int i = 0; i < arguments.size() - 1; i++)
                sb.append(arguments.get(i).toString()).append(", ");
            sb.append(arguments.get(arguments.size() - 1).toString());
            sb.append(")");
        }
        return sb.toString();
    }
}
