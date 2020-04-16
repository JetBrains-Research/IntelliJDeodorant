package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.*;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.MethodBodyObject;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.TypeCheckCodeFragmentAnalyzer;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.typeStateChecking.TypeCheckElimination;

import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.toPointer;

public class ClassObject extends ClassDeclarationObject {

    private final List<ConstructorObject> constructorList;
    private final List<EnumConstantDeclarationObject> enumConstantDeclarationList;
    private String superclass;
    private final List<String> interfaceList;
    private boolean _abstract;
    private boolean _interface;
    private boolean _static;
    private boolean _enum;
    private Access access;
    private SmartPsiElementPointer<PsiElement> typeDeclaration;
    private final String psiType;
    private final SmartPsiElementPointer<PsiElement> psiFile;
    private final SmartPsiElementPointer<PsiElement> psiClass;

    public ClassObject(PsiClass psiClass) {
        this.psiType = psiClass.getQualifiedName();
        this.name = psiClass.getName();
        this.constructorList = new ArrayList<>();
        this.interfaceList = new ArrayList<>();
        this.enumConstantDeclarationList = new ArrayList<>();
        this._abstract = psiClass.hasModifierProperty(PsiModifier.ABSTRACT);
        this._interface = psiClass.isInterface();
        this._static = psiClass.hasModifierProperty(PsiModifier.STATIC);
        this._enum = psiClass.isEnum();
        this.access = Access.NONE;
        this.typeDeclaration = toPointer(psiClass);
        this.psiFile = toPointer(psiClass.getContainingFile());
        this.psiClass = toPointer(psiClass);
    }

    public void setAbstractTypeDeclaration(PsiDeclarationStatement typeDeclaration) {
        this.typeDeclaration = toPointer(typeDeclaration);
    }

    public PsiElement getAbstractTypeDeclaration() {
        return typeDeclaration.getElement();
    }

    public ClassObject getClassObject() {
        return this;
    }

    public String getPsiType() {
        return psiType;
    }

    public PsiJavaFile getPsiFile() {
        return (PsiJavaFile) psiFile.getElement();
    }


    public List<TypeCheckElimination> generateTypeCheckEliminations() {
        List<TypeCheckElimination> typeCheckEliminations = new ArrayList<>();
        if (!_enum) {
            for (MethodObject methodObject : methodList) {
                MethodBodyObject methodBodyObject = methodObject.getMethodBody();
                if (methodBodyObject != null) {
                    List<TypeCheckElimination> list = methodBodyObject.generateTypeCheckEliminations();
                    for (TypeCheckElimination typeCheckElimination : list) {
                        if (!typeCheckElimination.allTypeCheckBranchesAreEmpty()) {
                            TypeCheckCodeFragmentAnalyzer analyzer = new TypeCheckCodeFragmentAnalyzer(
                                    typeCheckElimination,
                                    (PsiClass) getAbstractTypeDeclaration(),
                                    methodObject.getMethodDeclaration()
                            );
                            boolean hasTypeLocalVariableFieldOrMethod = typeCheckElimination.getTypeField() != null
                                    || typeCheckElimination.getTypeLocalVariable() != null
                                    || typeCheckElimination.getTypeMethodInvocation() != null;
                            if (hasTypeLocalVariableFieldOrMethod
                                    && typeCheckElimination.allTypeCheckingsContainStaticFieldOrSubclassType()
                                    && typeCheckElimination.isApplicable()) {
                                typeCheckEliminations.add(typeCheckElimination);
                            }
                        }
                    }
                }
            }
        }
        return typeCheckEliminations;
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public Access getAccess() {
        return access;
    }

    public void setSuperclass(String superclass) {
        this.superclass = superclass;
    }

    public void addInterface(String i) {
        interfaceList.add(i);
    }

    public void addConstructor(ConstructorObject c) {
        constructorList.add(c);
    }

    private ListIterator<ConstructorObject> getConstructorIterator() {
        return constructorList.listIterator();
    }

    public ListIterator<String> getInterfaceIterator() {
        return interfaceList.listIterator();
    }

    public String getSuperclass() {
        return superclass;
    }

    public void setAbstract(boolean abstr) {
        this._abstract = abstr;
    }

    public boolean isAbstract() {
        return this._abstract;
    }

    public void setInterface(boolean i) {
        this._interface = i;
    }

    public boolean isInterface() {
        return this._interface;
    }

    public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
    }

    public boolean isEnum() {
        return _enum;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!access.equals(Access.NONE))
            sb.append(access.toString()).append(" ");
        if (_static)
            sb.append("static").append(" ");
        if (_interface)
            sb.append("interface").append(" ");
        else if (_abstract)
            sb.append("abstract class").append(" ");
        else
            sb.append("class").append(" ");
        sb.append(name).append(" ");
        sb.append("extends ").append(superclass);
        if (!interfaceList.isEmpty()) {
            sb.append(" ").append("implements ");
            for (int i = 0; i < interfaceList.size() - 1; i++)
                sb.append(interfaceList.get(i)).append(", ");
            sb.append(interfaceList.get(interfaceList.size() - 1));
        }
        sb.append("\n\n").append("Fields:");
        for (FieldObject field : fieldList)
            sb.append("\n").append(field.toString());

        sb.append("\n\n").append("Constructors:");
        for (ConstructorObject constructor : constructorList)
            sb.append("\n").append(constructor.toString());

        sb.append("\n\n").append("Methods:");
        for (MethodObject method : methodList)
            sb.append("\n").append(method.toString());

        return sb.toString();
    }

    public PsiClass getPsiClass() {
        return (PsiClass) psiClass.getElement();
    }
}