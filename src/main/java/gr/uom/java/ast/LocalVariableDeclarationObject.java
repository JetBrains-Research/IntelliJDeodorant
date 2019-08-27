package gr.uom.java.ast;

import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;

public class LocalVariableDeclarationObject extends VariableDeclarationObject {
	private TypeObject type;
    private String name;
    private PsiDeclarationStatement variableDeclaration;
   // private ASTInformation variableDeclaration;
    private volatile int hashCode = 0;

    public LocalVariableDeclarationObject(TypeObject type, String name) {
        this.type = type;
        this.name = name;
    }

    public TypeObject getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public PsiDeclarationStatement getVariableDeclaration() {
    	return variableDeclaration;
	}

	public void setVariableDeclaration(PsiDeclarationStatement variableDeclaration) {
		//this.variableDeclaration = variableDeclaration;
		//this.variableBindingKey = variableDeclaration.resolveBinding().getKey();
		this.variableDeclaration = variableDeclaration;
	}

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if (o instanceof LocalVariableDeclarationObject) {
        	LocalVariableDeclarationObject lvdo = (LocalVariableDeclarationObject)o;
            return this.name.equals(lvdo.name) && this.type.equals(lvdo.type) &&
            		this.variableBindingKey.equals(lvdo.variableBindingKey);
        }
        return false;
    }

    public boolean equals(LocalVariableInstructionObject lvio) {
    	return this.name.equals(lvio.getName()) && this.type.equals(lvio.getType()) && this.variableBindingKey.equals(lvio.getVariableBindingKey());
    }

    public int hashCode() {
    	if(hashCode == 0) {
    		int result = 17;
    		result = 37*result + type.hashCode();
    		result = 37*result + name.hashCode();
    		result = 37*result + variableBindingKey.hashCode();
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
