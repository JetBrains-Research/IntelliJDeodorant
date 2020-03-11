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
    private TypeObject superclass;
    private final List<TypeObject> interfaceList;
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

    private boolean isFriend(String className) {
        if (superclass != null) {
            if (superclass.getClassType().equals(className))
                return true;
        }
        for (TypeObject interfaceType : interfaceList) {
            if (interfaceType.getClassType().equals(className))
                return true;
        }
        for (FieldObject field : fieldList) {
            TypeObject fieldType = field.getType();
            if (checkFriendship(fieldType, className))
                return true;
        }
        for (ConstructorObject constructor : constructorList) {
            ListIterator<ParameterObject> parameterIterator = constructor.getParameterListIterator();
            while (parameterIterator.hasNext()) {
                ParameterObject parameter = parameterIterator.next();
                TypeObject parameterType = parameter.getType();
                if (checkFriendship(parameterType, className))
                    return true;
            }
            for (CreationObject creation : constructor.getCreations()) {
                TypeObject creationType = creation.getType();
                if (checkFriendship(creationType, className))
                    return true;
            }
        }
        for (MethodObject method : methodList) {
            TypeObject returnType = method.getReturnType();
            if (checkFriendship(returnType, className))
                return true;
            ListIterator<ParameterObject> parameterIterator = method.getParameterListIterator();
            while (parameterIterator.hasNext()) {
                ParameterObject parameter = parameterIterator.next();
                TypeObject parameterType = parameter.getType();
                if (checkFriendship(parameterType, className))
                    return true;
            }
            for (CreationObject creation : method.getCreations()) {
                TypeObject creationType = creation.getType();
                if (checkFriendship(creationType, className))
                    return true;
            }
        }
        if (superclass != null) {
            ClassObject superclassObject = ASTReader.getSystemObject().getClassObject(superclass.getClassType());
            if (superclassObject != null)
                return superclassObject.isFriend(className);
        }
        return false;
    }

    private boolean checkFriendship(TypeObject type, String className) {
        if (type.getClassType().equals(className))
            return true;
        return type.getGenericType() != null && type.getGenericType().contains(className);
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

    public void setSuperclass(TypeObject superclass) {
        this.superclass = superclass;
    }

    public void addInterface(TypeObject i) {
        interfaceList.add(i);
    }

    public void addConstructor(ConstructorObject c) {
        constructorList.add(c);
    }

    public boolean addEnumConstantDeclaration(EnumConstantDeclarationObject f) {
        return enumConstantDeclarationList.add(f);
    }

    private ListIterator<ConstructorObject> getConstructorIterator() {
        return constructorList.listIterator();
    }

    public ListIterator<TypeObject> getInterfaceIterator() {
        return interfaceList.listIterator();
    }

    public ListIterator<TypeObject> getSuperclassIterator() {
        List<TypeObject> superclassList = new ArrayList<>(interfaceList);
        superclassList.add(superclass);
        return superclassList.listIterator();
    }

    public ListIterator<EnumConstantDeclarationObject> getEnumConstantDeclarationIterator() {
        return enumConstantDeclarationList.listIterator();
    }

    public TypeObject getSuperclass() {
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

    public void setEnum(boolean _enum) {
        this._enum = _enum;
    }

    public ConstructorObject getConstructor(ClassInstanceCreationObject cico) {
        ListIterator<ConstructorObject> ci = getConstructorIterator();
        while (ci.hasNext()) {
            ConstructorObject co = ci.next();
            if (co.equals(cico))
                return co;
        }
        return null;
    }

    public ConstructorObject getConstructor(ConstructorInvocationObject cio) {
        ListIterator<ConstructorObject> ci = getConstructorIterator();
        while (ci.hasNext()) {
            ConstructorObject co = ci.next();
            if (co.equals(cio))
                return co;
        }
        return null;
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