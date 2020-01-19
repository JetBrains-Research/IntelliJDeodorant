package org.jetbrains.research.intellijdeodorant.ide.refactoring.extractclass;

import com.intellij.psi.*;

import java.util.*;

//TODO comment I guess
public class ExtractClassPreviewProcessor {
    private List<PsiMethodPair> methodComparingList = new ArrayList<>();
    private Map<PsiMethod, ArrayList<PsiElementPair>> methodElementsComparingMap = new LinkedHashMap<>();

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

    public void putIntoMethodComparingMap(PsiMethod method, PsiElement originalElement, PsiElement newElement) {
        ArrayList<PsiElementPair> comparingList;
        if (!methodElementsComparingMap.containsKey(method)) {
            comparingList = new ArrayList<>();
            methodElementsComparingMap.put(method, comparingList);
        } else {
            comparingList = methodElementsComparingMap.get(method);
        }

        comparingList.add(new PsiElementPair(originalElement, newElement, method));
    }

    public List<PsiMethodPair> getMethodComparingList() {
        return methodComparingList;
    }

    public Map<PsiMethod, ArrayList<PsiElementPair>> getMethodElementsComparingMap() {
        return methodElementsComparingMap;
    }

    public void removeUnchangedMethods() {
        for (Iterator<PsiMethodPair> it = methodComparingList.iterator(); it.hasNext();) {
            PsiMethodPair methodPair = it.next();

            List<PsiElementPair> changedElements = methodElementsComparingMap.get(methodPair.getInitialPsiMethod());
            if (changedElements.size() == 0) {
                methodElementsComparingMap.remove(methodPair.getInitialPsiMethod());
                it.remove();
            }
        }
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

    public static class PsiMethodPair {
        private PsiMethod initialPsiMethod;
        private PsiMethod updatedPsiMethod;

        public PsiMethodPair(PsiMethod initialPsiMethod, PsiMethod updatedPsiMethod) {
            this.initialPsiMethod = initialPsiMethod;
            this.updatedPsiMethod = updatedPsiMethod;
        }

        public PsiMethod getInitialPsiMethod() {
            return initialPsiMethod;
        }

        public PsiMethod getUpdatedPsiMethod() {
            return updatedPsiMethod;
        }
    }

    public static class PsiElementPair {
        private PsiElement initialPsiElement;
        private PsiElement updatedPsiElement;
        private PsiMethod initialMethod;

        public PsiElementPair(PsiElement initialPsiElement, PsiElement updatedPsiElement, PsiMethod initialMethod) {
            this.initialPsiElement = initialPsiElement;
            this.updatedPsiElement = updatedPsiElement;
            this.initialMethod = initialMethod;
        }

        public PsiElement getInitialPsiElement() {
            return initialPsiElement;
        }

        public PsiElement getUpdatedPsiElement() {
            return updatedPsiElement;
        }

        public PsiMethod getInitialMethod() {
            return initialMethod;
        }

        public void setInitialPsiElement(PsiElement initialPsiElement) {
            this.initialPsiElement = initialPsiElement;
        }

        public void setUpdatedPsiElement(PsiElement updatedPsiElement) {
            this.updatedPsiElement = updatedPsiElement;
        }
    }
}
