package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.SmartPsiElementPointer;

public abstract class CreationObject {
	private final String type;
	SmartPsiElementPointer<PsiExpression> creation;
	
	CreationObject(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}
}
