package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiNewExpression;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class ArrayCreationObject extends CreationObject {

    public ArrayCreationObject(String type) {
        super(type);
    }

    public PsiNewExpression getArrayCreation() {
        return (PsiNewExpression) this.creation.getElement();
    }

    public void setArrayCreation(PsiNewExpression creation) {
        this.creation = toPointer(creation);
    }
}
