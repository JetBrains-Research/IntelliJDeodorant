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
            classObject.setSuperclass(psiClass.getSuperClass().getQualifiedName());
        }

        PsiClass[] superInterfaceTypes = psiClass.getInterfaces();
        for (PsiClass interfaceType : superInterfaceTypes) {
            classObject.addInterface(interfaceType.getQualifiedName());
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
        classObject.addField(fieldDeclaration);
    }

    private void processMethodDeclaration(final ClassObject classObject, PsiMethod methodDeclaration) {
        String methodName = methodDeclaration.getName();
        final ConstructorObject constructorObject = new ConstructorObject();
        constructorObject.setMethodDeclaration(methodDeclaration);
        constructorObject.setName(methodName);
        constructorObject.setClassName(classObject.getName());
        int methodDeclarationStartPosition = methodDeclaration.getStartOffsetInParent();
        int methodDeclarationEndPosition = methodDeclarationStartPosition + methodDeclaration.getTextLength();

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
            ParameterObject parameterObject = new ParameterObject(parameter.getType(), parameter.getName(), parameter.isVarArgs());
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
            methodObject.setReturnType(returnType);
            if (methodDeclaration.hasModifierProperty(PsiModifier.ABSTRACT))
                methodObject.setAbstract(true);
            if (methodDeclaration.hasModifierProperty(PsiModifier.STATIC))
                methodObject.setStatic(true);
            if (methodDeclaration.hasModifierProperty(PsiModifier.SYNCHRONIZED))
                methodObject.setSynchronized(true);
            if (methodDeclaration.hasModifierProperty(PsiModifier.NATIVE))
                methodObject.setNative(true);

            classObject.addMethod(methodObject);
            PsiField fieldInstruction = methodObject.isGetter();
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

}