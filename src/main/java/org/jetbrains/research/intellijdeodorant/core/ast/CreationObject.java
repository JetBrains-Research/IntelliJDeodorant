package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.SmartPsiElementPointer;

public abstract class CreationObject {
	private final TypeObject type;
	SmartPsiElementPointer<PsiExpression> creation;
	
	CreationObject(TypeObject type) {
		this.type = type;
	}

	public TypeObject getType() {
		return type;
	}
}
