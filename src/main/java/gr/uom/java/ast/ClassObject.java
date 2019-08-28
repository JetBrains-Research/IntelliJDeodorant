package gr.uom.java.ast;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.*;

import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

public class ClassObject extends gr.uom.java.ast.ClassDeclarationObject {

    private List<gr.uom.java.ast.ConstructorObject> constructorList;
    private List<gr.uom.java.ast.EnumConstantDeclarationObject> enumConstantDeclarationList;
    private TypeObject superclass;
    private List<TypeObject> interfaceList;
    private boolean _abstract;
    private boolean _interface;
    private boolean _static;
    private boolean _enum;
    private gr.uom.java.ast.Access access;
    private gr.uom.java.ast.ASTInformation typeDeclaration;
    private PsiFile iFile;

    public ClassObject(PsiClass psiClass) {
        this.name = psiClass.getName();
        this.constructorList = new ArrayList<gr.uom.java.ast.ConstructorObject>();
        this.interfaceList = new ArrayList<TypeObject>();
        this.enumConstantDeclarationList = new ArrayList<gr.uom.java.ast.EnumConstantDeclarationObject>();
        this._abstract = psiClass.hasModifier(JvmModifier.ABSTRACT);
        this._interface = psiClass.isInterface();
        this._static = psiClass.hasModifier(JvmModifier.STATIC);
        this._enum = psiClass.isEnum();
        this.access = gr.uom.java.ast.Access.NONE;
    }

    public void setAbstractTypeDeclaration(PsiDeclarationStatement typeDeclaration) {
        this.typeDeclaration = gr.uom.java.ast.ASTInformationGenerator.generateASTInformation(typeDeclaration);
    }

    public ASTInformation getAbstractTypeDeclaration() {
        return typeDeclaration;
    }

    public ClassObject getClassObject() {
        return this;
    }

    public PsiType getITypeRoot() {
        return typeDeclaration.getITypeRoot();
    }

    public PsiFile getIFile() {
        return iFile;
    }

    public void setIFile(PsiFile file) {
        iFile = file;
    }

    public boolean isFriend(String className) {
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
        if (type.getGenericType() != null && type.getGenericType().contains(className))
            return true;
        return false;
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

    public boolean addInterface(TypeObject i) {
        return interfaceList.add(i);
    }

    public boolean addConstructor(ConstructorObject c) {
        return constructorList.add(c);
    }

    public boolean addEnumConstantDeclaration(EnumConstantDeclarationObject f) {
        return enumConstantDeclarationList.add(f);
    }

    public ListIterator<ConstructorObject> getConstructorIterator() {
        return constructorList.listIterator();
    }

    public ListIterator<TypeObject> getInterfaceIterator() {
        return interfaceList.listIterator();
    }

    public ListIterator<TypeObject> getSuperclassIterator() {
        List<TypeObject> superclassList = new ArrayList<TypeObject>(interfaceList);
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
}