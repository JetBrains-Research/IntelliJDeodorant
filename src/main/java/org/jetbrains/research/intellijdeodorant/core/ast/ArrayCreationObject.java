package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiNewExpression;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class ArrayCreationObject extends CreationObject {

    public ArrayCreationObject(TypeObject type) {
        super(type);
    }

    public void setArrayCreation(PsiNewExpression creation) {
        this.creation = toPointer(creation);
    }
}
