package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class AnonymousClassDeclarationObject extends ClassDeclarationObject {
    private SmartPsiElementPointer<PsiElement> anonymousClassDeclaration;
    private ClassObject classObject;

    public void setAnonymousClassDeclaration(PsiAnonymousClass anonymous) {
        this.anonymousClassDeclaration = toPointer(anonymous);
    }

    public PsiAnonymousClass getAnonymousClassDeclaration() {
        return (PsiAnonymousClass) anonymousClassDeclaration.getElement();
    }

    public ClassObject getClassObject() {
        return classObject;
    }

    public void setClassObject(ClassObject classObject) {
        this.classObject = classObject;
    }

    public String getSuperclass() {
        return null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("\n\n").append("Fields:");
        for (FieldObject field : fieldList)
            sb.append("\n").append(field.toString());

        sb.append("\n\n").append("Methods:");
        for (MethodObject method : methodList)
            sb.append("\n").append(method.toString());

        return sb.toString();
    }
}
