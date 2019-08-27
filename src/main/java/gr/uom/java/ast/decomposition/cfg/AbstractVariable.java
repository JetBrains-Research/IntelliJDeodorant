package gr.uom.java.ast.decomposition.cfg;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiVariable;

import java.util.Arrays;

public abstract class AbstractVariable {
	//protected VariableDeclaration name;
	protected String variableBindingKey;
	protected String variableName;
	protected String variableType;
	protected boolean isField;
	protected boolean isParameter;
	protected boolean isStatic;
//TODO: rewrite it
	public AbstractVariable(PsiElement name) {
		//IVariableBinding variableBinding = name.resolveBinding();
		this.variableBindingKey = name.getReference().getElement().getText();
		this.variableName = name.getReference().getElement().getText();
		this.variableType = name.getReference().getElement().getText();
		this.isField = name.isValid();
		this.isParameter = false;
		this.isStatic = ((PsiVariable) name).hasModifier(JvmModifier.STATIC);
	}

	public AbstractVariable(PsiVariable variableBinding) {
		this(variableBinding.getName(), variableBinding.getName(), variableBinding.getType().getCanonicalText(),
				Arrays.asList(variableBinding.getClass().getFields()).contains(variableBinding), 
				/*variableBinding.isParameter()*/false, (variableBinding.hasModifier(JvmModifier.STATIC)));
	}

	public AbstractVariable(String variableBindingKey, String variableName, String variableType, boolean isField, boolean isParameter, boolean isStatic) {
		this.variableBindingKey = variableBindingKey;
		this.variableName = variableName;
		this.variableType = variableType;
		this.isField = isField;
		this.isParameter = isParameter;
		this.isStatic = isStatic;
	}

	public String getVariableBindingKey() {
		return variableBindingKey;
	}

	public String getVariableName() {
		return variableName;
	}

	public String getVariableType() {
		return variableType;
	}

	public boolean isField() {
		return isField;
	}

	public boolean isParameter() {
		return isParameter;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public abstract boolean containsPlainVariable(PlainVariable variable);
	public abstract boolean startsWithVariable(AbstractVariable variable);
	public abstract PlainVariable getInitialVariable();
}
