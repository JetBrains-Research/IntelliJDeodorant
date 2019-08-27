package gr.uom.java.ast;

import com.intellij.psi.PsiArrayInitializerExpression;
import com.intellij.psi.PsiNewExpression;

public class ArrayCreationObject extends CreationObject {

	public ArrayCreationObject(TypeObject type) {
		super(type);
	}

	public PsiNewExpression getArrayCreation() {
		return (PsiNewExpression)this.creation.recoverASTNode();
	}

	public void setArrayCreation(PsiNewExpression creation) {
		this.creation = ASTInformationGenerator.generateASTInformation(creation);
	}
}
