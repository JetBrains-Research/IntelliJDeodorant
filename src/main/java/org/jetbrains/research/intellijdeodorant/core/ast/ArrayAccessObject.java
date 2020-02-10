package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiArrayAccessExpression;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.SmartPsiElementPointer;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

class ArrayAccessObject {
    private final TypeObject type;
    private SmartPsiElementPointer<PsiExpression> arrayAccess;

    public ArrayAccessObject(TypeObject type) {
        this.type = type;
    }

    public TypeObject getType() {
        return type;
    }

    public PsiArrayAccessExpression getArrayAccess() {
        return (PsiArrayAccessExpression) this.arrayAccess.getElement();
    }

    public void setArrayAccess(PsiArrayAccessExpression arrayAccess) {
        this.arrayAccess = toPointer(arrayAccess);
    }
}
