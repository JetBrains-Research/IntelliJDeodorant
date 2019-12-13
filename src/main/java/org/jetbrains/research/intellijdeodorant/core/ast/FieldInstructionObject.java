package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiElement;

public class FieldInstructionObject {

    private final String ownerClass;
    private final TypeObject type;
    private final String name;
    private boolean _static;
    private PsiElement element;
    private volatile int hashCode = 0;

    public FieldInstructionObject(String ownerClass, TypeObject type, String name, PsiElement element) {
        this.ownerClass = ownerClass;
        this.type = type;
        this.name = name;
        this._static = false;
        this.element = element;
    }

    public String getOwnerClass() {
        return ownerClass;
    }

    public TypeObject getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
    }

    public PsiElement getElement() {
        return this.element;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof FieldInstructionObject) {
            FieldInstructionObject fio = (FieldInstructionObject) o;
            return this.ownerClass.equals(fio.ownerClass)
                    && this.name.equals(fio.name)
                    && this.type.equals(fio.type);
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + ownerClass.hashCode();
            result = 37 * result + name.hashCode();
            result = 37 * result + type.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        return ownerClass + "::" + name;
    }
}
