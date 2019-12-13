package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiArrayAccessExpression;

class ArrayAccessObject {
    private final TypeObject type;
    private ASTInformation arrayAccess;
	
    public ArrayAccessObject(TypeObject type) {
        this.type = type;
    }

    public TypeObject getType() {
        return type;
    }

    public PsiArrayAccessExpression getArrayAccess() {
        return (PsiArrayAccessExpression) this.arrayAccess.recoverASTNode();
    }

    public void setArrayAccess(PsiArrayAccessExpression arrayAccess) {
        this.arrayAccess = ASTInformationGenerator.generateASTInformation(arrayAccess);
    }
}
