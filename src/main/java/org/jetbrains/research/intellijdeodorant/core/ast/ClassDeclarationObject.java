package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiField;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.CatchClauseObject;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.TryStatementObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public abstract class ClassDeclarationObject {
    String name;
    final List<MethodObject> methodList;
    final List<PsiField> fieldList;

    ClassDeclarationObject() {
        this.methodList = new ArrayList<>();
        this.fieldList = new ArrayList<>();
    }

    public abstract ClassObject getClassObject();

    protected abstract String getSuperclass();

    public void addMethod(MethodObject method) {
        methodList.add(method);
    }

    public void addField(PsiField f) {
        fieldList.add(f);
    }

    public List<MethodObject> getMethodList() {
        return methodList;
    }

    public ListIterator<MethodObject> getMethodIterator() {
        return methodList.listIterator();
    }

    public ListIterator<PsiField> getFieldIterator() {
        return fieldList.listIterator();
    }

    public int getNumberOfMethods() {
        return methodList.size();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean containsMethodWithTestAnnotation() {
        for (MethodObject method : methodList) {
            if (method.hasTestAnnotation())
                return true;
        }
        return false;
    }

    public MethodObject getMethod(MethodInvocationObject mio) {
        ListIterator<MethodObject> mi = getMethodIterator();
        while (mi.hasNext()) {
            MethodObject mo = mi.next();
            if (mo.equals(mio))
                return mo;
        }
        return null;
    }

    public MethodObject getMethod(SuperMethodInvocationObject smio) {
        ListIterator<MethodObject> mi = getMethodIterator();
        while (mi.hasNext()) {
            MethodObject mo = mi.next();
            if (mo.equals(smio))
                return mo;
        }
        return null;
    }

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation) {
        for (MethodObject method : methodList) {
            if (method.containsMethodInvocation(methodInvocation))
                return true;
        }
        return false;
    }

    public boolean containsFieldInstruction(PsiField fieldInstruction) {
        for (MethodObject method : methodList) {
            if (method.containsFieldInstruction(fieldInstruction))
                return true;
        }
        return false;
    }

    public boolean containsMethodInvocation(MethodInvocationObject methodInvocation, MethodObject excludedMethod) {
        for (MethodObject method : methodList) {
            if (!method.equals(excludedMethod) && method.containsMethodInvocation(methodInvocation))
                return true;
        }
        return false;
    }

    public boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation) {
        for (MethodObject method : methodList) {
            if (method.containsSuperMethodInvocation(superMethodInvocation))
                return true;
        }
        return false;
    }

    public Set<PsiField> getFieldsAccessedInsideMethod(AbstractMethodDeclaration method) {
        Set<PsiField> fields = new LinkedHashSet<>();
        for (PsiField fieldInstruction : method.getFieldInstructions()) {
            PsiField accessedFieldFromThisClass = findField(fieldInstruction);
            if (accessedFieldFromThisClass != null) {
                fields.add(accessedFieldFromThisClass);
            }
        }
        if (method.getMethodBody() != null) {
            List<TryStatementObject> tryStatements = method.getMethodBody().getTryStatements();
            for (TryStatementObject tryStatement : tryStatements) {
                for (CatchClauseObject catchClause : tryStatement.getCatchClauses()) {
                    for (PsiField fieldInstruction : catchClause.getBody().getFieldInstructions()) {
                        PsiField accessedFieldFromThisClass = findField(fieldInstruction);
                        if (accessedFieldFromThisClass != null) {
                            fields.add(accessedFieldFromThisClass);
                        }
                    }
                }
                if (tryStatement.getFinallyClause() != null) {
                    for (PsiField fieldInstruction : tryStatement.getFinallyClause().getFieldInstructions()) {
                        PsiField accessedFieldFromThisClass = findField(fieldInstruction);
                        if (accessedFieldFromThisClass != null) {
                            fields.add(accessedFieldFromThisClass);
                        }
                    }
                }
            }
        }
        return fields;
    }

    private PsiField getField(PsiField fieldInstruction) {
        for (PsiField field : fieldList) {
            if (field.equals(fieldInstruction)) {
                return field;
            }
        }
        return null;
    }

    PsiField findField(PsiField fieldInstruction) {
        PsiField field = getField(fieldInstruction);
        if (field != null) {
            return field;
        } else {
            String superclassType = getSuperclass();
            if (superclassType != null) {
                ClassObject superclassObject = ASTReader.getSystemObject().getClassObject(superclassType);
                if (superclassObject != null) {
                    return superclassObject.findField(fieldInstruction);
                }
            }
        }
        return null;
    }

}
