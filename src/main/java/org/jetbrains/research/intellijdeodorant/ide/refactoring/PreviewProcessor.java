package org.jetbrains.research.intellijdeodorant.ide.refactoring;

import com.intellij.psi.*;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

//TODO comment I guess
public class PreviewProcessor {
    private Map<PsiMethod, PsiMethod> methodComparingMap = new LinkedHashMap<>();

    public static SourceFileAndClass cloneSourceFile(PsiJavaFile sourceFile, PsiClass sourceTypeDeclaration, Set<PsiField> extractedFieldFragments, Set<PsiMethod> extractedMethods, Set<PsiMethod> delegateMethods) {
        PsiJavaFile copyFile = (PsiJavaFile) sourceFile.copy();
        Map<PsiMethod, PsiMethod> sourceToCopyMethodMap = new LinkedHashMap<>();
        Map<PsiField, PsiField> sourceToCopyFieldMap = new LinkedHashMap<>();
        Map<PsiClass, PsiClass> sourceToCopyClassMap = new LinkedHashMap<>();

        mapSourceFileAndFieldsToCopyFile(sourceFile, copyFile, sourceToCopyMethodMap, sourceToCopyFieldMap, sourceToCopyClassMap);

        sourceFile = copyFile;
        sourceTypeDeclaration = sourceToCopyClassMap.get(sourceTypeDeclaration);

        Set<PsiField> copyExtractedFields = new LinkedHashSet<>();
        for (PsiField field : extractedFieldFragments) {
            copyExtractedFields.add(sourceToCopyFieldMap.get(field));
        }

        Set<PsiMethod> copyExtractedMethods = new LinkedHashSet<>();
        for (PsiMethod method : extractedMethods) {
            copyExtractedMethods.add(sourceToCopyMethodMap.get(method));
        }

        Set<PsiMethod> copyDelegateMethods = new LinkedHashSet<>();
        for (PsiMethod method : delegateMethods) {
            copyDelegateMethods.add(sourceToCopyMethodMap.get(method));
        }

        extractedFieldFragments.clear();
        extractedFieldFragments.addAll(copyExtractedFields);

        extractedMethods.clear();
        extractedMethods.addAll(copyExtractedMethods);

        delegateMethods.clear();
        delegateMethods.addAll(copyDelegateMethods);

        return new SourceFileAndClass(sourceFile, sourceTypeDeclaration);
    }

    private static void mapSourceFileAndFieldsToCopyFile(PsiElement sourceElement, PsiElement copyElement, Map<PsiMethod, PsiMethod> methodMap, Map<PsiField, PsiField> fieldMap, Map<PsiClass, PsiClass> classMap) {
        if (sourceElement instanceof PsiMethod && copyElement instanceof PsiMethod) {
            methodMap.put((PsiMethod) sourceElement, (PsiMethod) copyElement);
        }

        if (sourceElement instanceof PsiField && copyElement instanceof PsiField) {
            fieldMap.put((PsiField) sourceElement, (PsiField) copyElement);
        }

        if (sourceElement instanceof PsiClass && copyElement instanceof PsiClass) {
            classMap.put((PsiClass) sourceElement, (PsiClass) copyElement);
        }

        for (int i = 0; i < sourceElement.getChildren().length; i++) {
            mapSourceFileAndFieldsToCopyFile(sourceElement.getChildren()[i], copyElement.getChildren()[i], methodMap, fieldMap, classMap);
        }
    }

    public Map<PsiMethod, PsiMethod> getMethodComparingMap() {
        return methodComparingMap;
    }

    public static class SourceFileAndClass {
        private final PsiJavaFile sourceFile;
        private final PsiClass sourceTypeDeclaration;

        public SourceFileAndClass(PsiJavaFile sourceFile, PsiClass sourceTypeDeclaration) {
            this.sourceFile = sourceFile;
            this.sourceTypeDeclaration = sourceTypeDeclaration;
        }

        public PsiClass getSourceTypeDeclaration() {
            return sourceTypeDeclaration;
        }

        public PsiJavaFile getSourceFile() {
            return sourceFile;
        }
    }
}
