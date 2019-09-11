package core.ast;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.*;

import core.ast.decomposition.MethodBodyObject;
import core.distance.ProjectInfo;

import java.util.*;

public class ASTReader {

    private static SystemObject systemObject;
    private static ProjectInfo examinedProject;

    public ASTReader(ProjectInfo project) {

        systemObject = new SystemObject();
        examinedProject = project;
        List<PsiClass> classes = project.getClasses();
        for (PsiClass c : classes) {
            systemObject.addClass(processTypeDeclaration(c));
        }
    }

    public ASTReader(ProjectInfo iJavaProject, SystemObject existingSystemObject) {
        examinedProject = iJavaProject;
        systemObject = existingSystemObject;
        List<PsiClass> classes = iJavaProject.getClasses();
        for (PsiClass c : classes) {
            systemObject.addClass(processTypeDeclaration(c));
        }
    }

    private ClassObject processTypeDeclaration(PsiClass psiClass) {
        final ClassObject classObject = new ClassObject(psiClass);
        classObject.setName(psiClass.getQualifiedName());
        if (psiClass.isInterface()) {
            classObject.setInterface(true);
        }
        if ((psiClass.hasModifier(JvmModifier.ABSTRACT)))
            classObject.setAbstract(true);

        if ((psiClass.hasModifier(JvmModifier.PUBLIC)))
            classObject.setAccess(Access.PUBLIC);
        else if ((psiClass.hasModifier(JvmModifier.PROTECTED)))
            classObject.setAccess(Access.PROTECTED);
        else if ((psiClass.hasModifier(JvmModifier.PRIVATE)))
            classObject.setAccess(Access.PRIVATE);
        else
            classObject.setAccess(Access.NONE);

        if ((psiClass.hasModifier(JvmModifier.STATIC)))
            classObject.setStatic(true);
        if (psiClass.getSuperClass() != null) {
            String superclassType = psiClass.getSuperClass().getQualifiedName();
            TypeObject typeObject = TypeObject.extractTypeObject(superclassType);
            classObject.setSuperclass(typeObject);
        }

        PsiClass[] superInterfaceTypes = psiClass.getInterfaces();
        for (PsiClass interfaceType : superInterfaceTypes) {
            TypeObject typeObject = TypeObject.extractTypeObject(interfaceType.getQualifiedName());
            classObject.addInterface(typeObject);
        }

        PsiField[] fieldDeclarations = psiClass.getFields();
        for (PsiField fieldDeclaration : fieldDeclarations) {
            processFieldDeclaration(classObject, fieldDeclaration);
        }

        PsiMethod[] methodDeclarations = psiClass.getMethods();
        for (PsiMethod methodDeclaration : methodDeclarations) {
            processMethodDeclaration(classObject, methodDeclaration);
        }
        return classObject;
    }

    private void processFieldDeclaration(final ClassObject classObject, PsiField fieldDeclaration) {
        List<CommentObject> fieldDeclarationComments = new ArrayList<>();
        int fieldDeclarationStartPosition = fieldDeclaration.getStartOffsetInParent();
        int fieldDeclarationEndPosition = fieldDeclarationStartPosition + fieldDeclaration.getTextLength();
        for (CommentObject comment : classObject.commentList) {
            int commentStartPosition = comment.getStartPosition();
            int commentEndPosition = commentStartPosition + comment.getLength();
            if (fieldDeclarationStartPosition <= commentStartPosition && fieldDeclarationEndPosition >= commentEndPosition) {
                fieldDeclarationComments.add(comment);
            }
        }

        TypeObject typeObject = TypeObject.extractTypeObject(fieldDeclaration.getType().getCanonicalText());
        typeObject.setArrayDimension(typeObject.getArrayDimension());
        FieldObject fieldObject = new FieldObject(typeObject, fieldDeclaration.getName());
        fieldObject.setClassName(classObject.getName());
        fieldObject.addComments(fieldDeclarationComments);

        if (fieldDeclaration.hasModifier(JvmModifier.PUBLIC))
            fieldObject.setAccess(Access.PUBLIC);
        else if (fieldDeclaration.hasModifier(JvmModifier.PROTECTED))
            fieldObject.setAccess(Access.PROTECTED);
        else if (fieldDeclaration.hasModifier(JvmModifier.PRIVATE))
            fieldObject.setAccess(Access.PRIVATE);
        else
            fieldObject.setAccess(Access.NONE);
        if (fieldDeclaration.hasModifier(JvmModifier.STATIC))
            fieldObject.setStatic(true);

        classObject.addField(fieldObject);

    }

    private void processMethodDeclaration(final ClassObject classObject, PsiMethod methodDeclaration) {
        String methodName = methodDeclaration.getName();
        final ConstructorObject constructorObject = new ConstructorObject();
        constructorObject.setMethodDeclaration(methodDeclaration);
        constructorObject.setName(methodName);
        constructorObject.setClassName(classObject.getName());
        int methodDeclarationStartPosition = methodDeclaration.getStartOffsetInParent();
        int methodDeclarationEndPosition = methodDeclarationStartPosition + methodDeclaration.getTextLength();
        for (CommentObject comment : classObject.commentList) {
            int commentStartPosition = comment.getStartPosition();
            int commentEndPosition = commentStartPosition + comment.getLength();
            if (methodDeclarationStartPosition <= commentStartPosition && methodDeclarationEndPosition >= commentEndPosition) {
                constructorObject.addComment(comment);
            }
        }

        if (methodDeclaration.hasModifier(JvmModifier.PUBLIC))
            constructorObject.setAccess(Access.PUBLIC);
        else if (methodDeclaration.hasModifier(JvmModifier.PROTECTED))
            constructorObject.setAccess(Access.PROTECTED);
        else if (methodDeclaration.hasModifier(JvmModifier.PRIVATE))
            constructorObject.setAccess(Access.PRIVATE);
        else
            constructorObject.setAccess(Access.NONE);

        PsiParameter[] parameters = methodDeclaration.getParameterList().getParameters();
        for (PsiParameter parameter : parameters) {
            String parameterType = parameter.getType().getCanonicalText();
            TypeObject typeObject = TypeObject.extractTypeObject(parameterType);
            if (parameter.isVarArgs()) {
                typeObject.setArrayDimension(1);
            }
            ParameterObject parameterObject = new ParameterObject(typeObject, parameter.getName(), parameter.isVarArgs());
            parameterObject.setSingleVariableDeclaration(parameter);
            constructorObject.addParameter(parameterObject);
        }

        PsiCodeBlock methodBody = methodDeclaration.getBody();
        if (methodBody != null) {
            MethodBodyObject methodBodyObject = new MethodBodyObject(methodBody);
            constructorObject.setMethodBody(methodBodyObject);
        }

        for (AnonymousClassDeclarationObject anonymous : constructorObject.getAnonymousClassDeclarations()) {
            anonymous.setClassObject(classObject);
            PsiAnonymousClass anonymousClassDeclaration = anonymous.getAnonymousClassDeclaration();
            int anonymousClassDeclarationStartPosition = anonymousClassDeclaration.getStartOffsetInParent();
            int anonymousClassDeclarationEndPosition = anonymousClassDeclarationStartPosition + anonymousClassDeclaration.getTextLength();
            for (CommentObject comment : constructorObject.commentList) {
                int commentStartPosition = comment.getStartPosition();
                int commentEndPosition = commentStartPosition + comment.getLength();
                if (anonymousClassDeclarationStartPosition <= commentStartPosition && anonymousClassDeclarationEndPosition >= commentEndPosition) {
                    anonymous.addComment(comment);
                }
            }
        }

        if (methodDeclaration.isConstructor()) {
            classObject.addConstructor(constructorObject);
        } else {
            MethodObject methodObject = new MethodObject(methodDeclaration, constructorObject);
            PsiAnnotation[] extendedModifiers = methodDeclaration.getAnnotations();
            for (PsiAnnotation extendedModifier : extendedModifiers) {
                if ("Test".equals(extendedModifier.getQualifiedName())) {
                    methodObject.setTestAnnotation(true);
                    break;
                }
            }
            PsiType returnType = methodDeclaration.getReturnType();
            String qualifiedName = returnType != null ? returnType.getCanonicalText() : null;
            TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
            methodObject.setReturnType(typeObject);
            if (methodDeclaration.hasModifier(JvmModifier.ABSTRACT))
                methodObject.setAbstract(true);
            if (methodDeclaration.hasModifier(JvmModifier.STATIC))
                methodObject.setStatic(true);
            if (methodDeclaration.hasModifier(JvmModifier.SYNCHRONIZED))
                methodObject.setSynchronized(true);
            if (methodDeclaration.hasModifier(JvmModifier.NATIVE))
                methodObject.setNative(true);

            classObject.addMethod(methodObject);
            FieldInstructionObject fieldInstruction = methodObject.isGetter();
            if (fieldInstruction != null)
                systemObject.addGetter(methodObject.generateMethodInvocation(), fieldInstruction);
            fieldInstruction = methodObject.isSetter();
            if (fieldInstruction != null)
                systemObject.addSetter(methodObject.generateMethodInvocation(), fieldInstruction);
            fieldInstruction = methodObject.isCollectionAdder();
            if (fieldInstruction != null)
                systemObject.addCollectionAdder(methodObject.generateMethodInvocation(), fieldInstruction);
            MethodInvocationObject methodInvocation = methodObject.isDelegate();
            if (methodInvocation != null)
                systemObject.addDelegate(methodObject.generateMethodInvocation(), methodInvocation);
        }
    }

    public static SystemObject getSystemObject() {
        return systemObject;
    }

    public static ProjectInfo getExaminedProject() {
        return examinedProject;
    }

}