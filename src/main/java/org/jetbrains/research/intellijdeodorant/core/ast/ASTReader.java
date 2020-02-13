package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.*;

import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.MethodBodyObject;
import org.jetbrains.research.intellijdeodorant.core.distance.ProjectInfo;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;

import java.util.*;

public class ASTReader {

    private static SystemObject systemObject;
    private static ProjectInfo examinedProject;

    public ASTReader(ProjectInfo project, ProgressIndicator indicator) {
        indicator.setText(IntelliJDeodorantBundle.message("feature.envy.parsing.indicator"));
        indicator.setFraction(0.0);
        systemObject = new SystemObject();
        examinedProject = project;
        List<PsiClass> classes = project.getClasses();
        int processedClasses = 0;
        int classesCount = classes.size();
        for (PsiClass psiClass : classes) {
            systemObject.addClass(processTypeDeclaration(psiClass));
            processedClasses += 1;
            indicator.setFraction((double) processedClasses / classesCount);
        }
        indicator.setFraction(1.0);
    }

    private ClassObject processTypeDeclaration(PsiClass psiClass) {
        final ClassObject classObject = new ClassObject(psiClass);
        classObject.setName(psiClass.getQualifiedName());
        if (psiClass.isInterface()) {
            classObject.setInterface(true);
        }
        if (psiClass.hasModifierProperty(PsiModifier.ABSTRACT))
            classObject.setAbstract(true);

        if (psiClass.hasModifierProperty(PsiModifier.PUBLIC))
            classObject.setAccess(Access.PUBLIC);
        else if (psiClass.hasModifierProperty(PsiModifier.PROTECTED))
            classObject.setAccess(Access.PROTECTED);
        else if (psiClass.hasModifierProperty(PsiModifier.PRIVATE))
            classObject.setAccess(Access.PRIVATE);
        else
            classObject.setAccess(Access.NONE);

        if (psiClass.hasModifierProperty(PsiModifier.STATIC))
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
        FieldObject fieldObject = new FieldObject(typeObject, fieldDeclaration.getName(), fieldDeclaration);
        fieldObject.setClassName(classObject.getName());
        fieldObject.addComments(fieldDeclarationComments);

        if (fieldDeclaration.hasModifierProperty(PsiModifier.PUBLIC))
            fieldObject.setAccess(Access.PUBLIC);
        else if (fieldDeclaration.hasModifierProperty(PsiModifier.PROTECTED))
            fieldObject.setAccess(Access.PROTECTED);
        else if (fieldDeclaration.hasModifierProperty(PsiModifier.PRIVATE))
            fieldObject.setAccess(Access.PRIVATE);
        else
            fieldObject.setAccess(Access.NONE);
        if (fieldDeclaration.hasModifierProperty(PsiModifier.STATIC))
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

        if (methodDeclaration.hasModifierProperty(PsiModifier.PUBLIC))
            constructorObject.setAccess(Access.PUBLIC);
        else if (methodDeclaration.hasModifierProperty(PsiModifier.PROTECTED))
            constructorObject.setAccess(Access.PROTECTED);
        else if (methodDeclaration.hasModifierProperty(PsiModifier.PRIVATE))
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
            if (methodDeclaration.hasModifierProperty(PsiModifier.ABSTRACT))
                methodObject.setAbstract(true);
            if (methodDeclaration.hasModifierProperty(PsiModifier.STATIC))
                methodObject.setStatic(true);
            if (methodDeclaration.hasModifierProperty(PsiModifier.SYNCHRONIZED))
                methodObject.setSynchronized(true);
            if (methodDeclaration.hasModifierProperty(PsiModifier.NATIVE))
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