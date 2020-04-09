package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.util.SmartList;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class FieldObject extends VariableDeclarationObject {

    private final String name;
    private final PsiType type;
    private boolean _static;
    private Access access;
    private String className;
    private final SmartPsiElementPointer<PsiElement> psiField;
    private volatile int hashCode = 0;

    public FieldObject(PsiType type, String fieldName, PsiField field) {
        this.type = type;
        this.name = fieldName;
        this._static = false;
        this.access = Access.NONE;
        this.psiField = toPointer(field);
    }

    private PsiField getVariableDeclarationFragment() {
        return (PsiField) psiField.getElement();
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public Access getAccess() {
        return access;
    }

    public String getName() {
        return name;
    }

    public PsiType getType() {
        return type;
    }

    public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof FieldObject) {
            FieldObject fieldObject = (FieldObject) o;
            return Objects.equals(getVariableDeclarationFragment(), fieldObject.getVariableDeclarationFragment());
        }
        return false;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return this.className;
    }

    public boolean equals(FieldInstructionObject fio) {
        return this.className.equals(fio.getOwnerClass())
                && this.name.equals(fio.getName())
                && this.type.equals(fio.getType());
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + className.hashCode();
            result = 37 * result + name.hashCode();
            result = 37 * result + type.hashCode();
            result = 37 * result + getVariableDeclarationFragment().hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!access.equals(Access.NONE))
            sb.append(access.toString()).append(" ");
        if (_static)
            sb.append("static").append(" ");
        sb.append(type.toString()).append(" ");
        sb.append(name);
        return sb.toString();
    }

    public PsiField getVariableDeclaration() {
        return getVariableDeclarationFragment();
    }

    public FieldInstructionObject generateFieldInstruction() {
        FieldInstructionObject fieldInstruction = new FieldInstructionObject(this.className, this.type, this.name, getVariableDeclarationFragment());
        fieldInstruction.setStatic(this._static);
        return fieldInstruction;
    }
}