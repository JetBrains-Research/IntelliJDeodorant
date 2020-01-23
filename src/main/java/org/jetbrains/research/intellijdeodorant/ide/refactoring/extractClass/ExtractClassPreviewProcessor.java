package org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass;

import com.intellij.psi.*;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;

import java.util.*;

public class ExtractClassPreviewProcessor {
    private List<PsiMethodComparingPair> methodComparingList = new ArrayList<>();
    private Map<PsiMethod, ArrayList<PsiElementComparingPair>> methodElementsComparingMap = new LinkedHashMap<>();
    private List<PsiElementChange> psiElementChanges = new ArrayList<>();
    private PsiClassWrapper initialSourceClass;
    private PsiClassWrapper updatedSourceClass;
    private PsiClassWrapper extractedClass;
    private Map<PsiElement, PsiElement> updatedSourceElementsToInitial = new HashMap<>();

    public CopiedData cloneSourceFile(PsiJavaFile sourceFile, PsiClass sourceTypeDeclaration, Set<PsiField> extractedFieldFragments, Set<PsiMethod> extractedMethods, Set<PsiMethod> delegateMethods) {
        PsiJavaFile copyFileToUpdate = (PsiJavaFile) sourceFile.copy();
        PsiJavaFile copyFileToCompareWithInitial = (PsiJavaFile) sourceFile.copy();

        Map<PsiElement, PsiElement> sourceFileToCopy = new HashMap<>();
        mapElementsToCopy(sourceFile, copyFileToUpdate, sourceFileToCopy);

        Set<PsiField> copyExtractedFields = new LinkedHashSet<>();
        for (PsiField field : extractedFieldFragments) {
            copyExtractedFields.add((PsiField) sourceFileToCopy.get(field));
        }

        Set<PsiMethod> copyExtractedMethods = new LinkedHashSet<>();
        for (PsiMethod method : extractedMethods) {
            copyExtractedMethods.add((PsiMethod) sourceFileToCopy.get(method));
        }

        Set<PsiMethod> copyDelegateMethods = new LinkedHashSet<>();
        for (PsiMethod method : delegateMethods) {
            copyDelegateMethods.add((PsiMethod) sourceFileToCopy.get(method));
        }

        extractedFieldFragments.clear();
        extractedFieldFragments.addAll(copyExtractedFields);

        extractedMethods.clear();
        extractedMethods.addAll(copyExtractedMethods);

        delegateMethods.clear();
        delegateMethods.addAll(copyDelegateMethods);

        mapElementsToCopy(copyFileToUpdate, copyFileToCompareWithInitial, updatedSourceElementsToInitial);
        PsiClass sourceClassCopyToUpdate = (PsiClass) sourceFileToCopy.get(sourceTypeDeclaration);

        return new CopiedData(copyFileToCompareWithInitial, (PsiClass) updatedSourceElementsToInitial.get(sourceClassCopyToUpdate), copyFileToUpdate, sourceClassCopyToUpdate);
    }

    public static void mapElementsToCopy(PsiElement sourceElement, PsiElement copyElement, Map<PsiElement, PsiElement> map) {
        map.put(sourceElement, copyElement);

        for (int i = 0; i < sourceElement.getChildren().length; i++) {
            mapElementsToCopy(sourceElement.getChildren()[i], copyElement.getChildren()[i], map);
        }
    }

    public List<PsiMethodComparingPair> getMethodComparingList() {
        return methodComparingList;
    }

    public Map<PsiMethod, ArrayList<PsiElementComparingPair>> getMethodElementsComparingMap() {
        return methodElementsComparingMap;
    }

    public void removeUnchangedMethods() {
        for (Iterator<PsiMethodComparingPair> it = methodComparingList.iterator(); it.hasNext(); ) {
            PsiMethodComparingPair methodPair = it.next();

            if (methodPair.forceComparing) {
                continue;
            }

            List<PsiElementComparingPair> changedElements = methodElementsComparingMap.get(methodPair.getInitialPsiMethod());
            if (changedElements == null) {
                it.remove();
                return;
            }

            if (changedElements.size() == 0) {
                methodElementsComparingMap.remove(methodPair.getInitialPsiMethod());
                it.remove();
            }
        }
    }

    public List<PsiElementChange> getPsiElementChanges() {
        return psiElementChanges;
    }

    public PsiClassWrapper getInitialSourceClass() {
        return initialSourceClass;
    }

    public void setInitialSourceClass(PsiClassWrapper initialSourceClass) {
        this.initialSourceClass = initialSourceClass;
    }

    public PsiClassWrapper getUpdatedSourceClass() {
        return updatedSourceClass;
    }

    public void setUpdatedSourceClass(PsiClassWrapper updatedSourceClass) {
        this.updatedSourceClass = updatedSourceClass;
    }

    public PsiClassWrapper getExtractedClass() {
        return extractedClass;
    }

    public void setExtractedClass(PsiClassWrapper extractedClass) {
        this.extractedClass = extractedClass;
    }

    public Map<PsiElement, PsiElement> getUpdatedSourceElementsToInitial() {
        return updatedSourceElementsToInitial;
    }

    public void addToPsiElementChangesFromUpdatedClass(PsiElementChange psiElementChange) {
        PsiElement element = psiElementChange.getPsiElement();
        if (element != null) {
            element = updatedSourceElementsToInitial.get(element);
            if (element != null) {
                //Can be null if this is a newly created element etc
                psiElementChange.setPsiElement(element);
            }
        }

        element = psiElementChange.getAnchor();
        if (element != null) {
            element = updatedSourceElementsToInitial.get(element);
            if (element != null) {
                //can be null
                psiElementChange.setAnchor(element);
            }
        }

        psiElementChanges.add(psiElementChange);
    }

    public void sortChanges() {
        methodComparingList.sort(Comparator.comparingInt(p -> p.getInitialPsiMethod().getTextOffset()));

        for (PsiMethod method : methodElementsComparingMap.keySet()) {
            ArrayList<PsiElementComparingPair> comparingPairs = methodElementsComparingMap.get(method);

            if (comparingPairs != null) {
                comparingPairs.sort(Comparator.comparingInt(p -> p.getInitialPsiElement().getTextOffset()));
            }
        }

        psiElementChanges.sort(Comparator.comparingInt(c -> c.getPsiElement().getTextOffset()));
    }

    public static class CopiedData {
        private final PsiJavaFile sourceFileInitialCopy;
        private final PsiClass sourceClassInitialCopy;
        private final PsiJavaFile fileToUpdate;
        private final PsiClass classToUpdate;

        public CopiedData(PsiJavaFile sourceFileInitialCopy, PsiClass sourceClassInitialCopy, PsiJavaFile fileToUpdate, PsiClass classToUpdate) {
            this.sourceFileInitialCopy = sourceFileInitialCopy;
            this.sourceClassInitialCopy = sourceClassInitialCopy;
            this.fileToUpdate = fileToUpdate;
            this.classToUpdate = classToUpdate;
        }

        public PsiJavaFile getSourceFileInitialCopy() {
            return sourceFileInitialCopy;
        }

        public PsiClass getSourceClassInitialCopy() {
            return sourceClassInitialCopy;
        }

        public PsiJavaFile getFileToUpdate() {
            return fileToUpdate;
        }

        public PsiClass getClassToUpdate() {
            return classToUpdate;
        }
    }

    public static class PsiMethodComparingPair {
        private PsiMethod initialPsiMethod;
        private PsiMethod updatedPsiMethod;
        private String description;
        private static final String UPDATED_METHOD = IntelliJDeodorantBundle.message("god.class.preview.updated.method");
        /**
         * Will compare even if the corresponding set in `methodElementsComparingMap` is empty. Use for methods that
         * does not update methodElementComparingMap.
         */
        private boolean forceComparing;

        public PsiMethodComparingPair(PsiMethod initialPsiMethod, PsiMethod updatedPsiMethod) {
            this(initialPsiMethod, updatedPsiMethod, null);
        }

        public PsiMethodComparingPair(PsiMethod initialPsiMethod, PsiMethod updatedPsiMethod, String description) {
            this(initialPsiMethod, updatedPsiMethod, description, false);
        }

        public PsiMethodComparingPair(PsiMethod initialPsiMethod, PsiMethod updatedPsiMethod, String description, boolean forceComparing) {
            this.initialPsiMethod = initialPsiMethod;
            this.updatedPsiMethod = updatedPsiMethod;
            this.description = description;
            this.forceComparing = forceComparing;
        }

        public PsiMethod getInitialPsiMethod() {
            return initialPsiMethod;
        }

        public PsiMethod getUpdatedPsiMethod() {
            return updatedPsiMethod;
        }

        @Override
        public String toString() {
            if (description != null) {
                return description;
            } else {
                return UPDATED_METHOD + " " + initialPsiMethod.getName();
            }
        }
    }

    public static class PsiElementComparingPair {
        private PsiElement initialPsiElement;
        private PsiElement updatedPsiElement;
        private PsiMethod initialMethod;
        private String description;

        public PsiElementComparingPair(PsiElement initialPsiElement, PsiElement updatedPsiElement, PsiMethod initialMethod, String description) {
            this.initialPsiElement = initialPsiElement;
            this.updatedPsiElement = updatedPsiElement;
            this.initialMethod = initialMethod;
            this.description = description;
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

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public static class PsiElementChange {
        private PsiElement psiElement;
        private CHANGE_TYPE change_type;
        private String description;
        private PsiElement anchor;

        public PsiElement getPsiElement() {
            return psiElement;
        }

        public void setPsiElement(PsiElement psiElement) {
            this.psiElement = psiElement;
        }

        public PsiElement getAnchor() {
            return anchor;
        }

        public void setAnchor(PsiElement anchor) {
            this.anchor = anchor;
        }

        public CHANGE_TYPE getChange_type() {
            return change_type;
        }

        public enum CHANGE_TYPE {
            REMOVE, ADD_AFTER, ADD_BEFORE, REPLACE
        }

        public PsiElementChange(PsiElement psiElement, CHANGE_TYPE change_type, String description) {
            this(psiElement, change_type, description, null);
        }

        public PsiElementChange(PsiElement psiElement, CHANGE_TYPE change_type, String description, PsiElement anchor) {
            this.psiElement = psiElement;
            this.change_type = change_type;
            this.description = description;
            this.anchor = anchor;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    public static class PsiClassWrapper {
        private PsiClass psiClass;
        private String description;

        public PsiClassWrapper(PsiClass psiClass, String description) {
            this.psiClass = psiClass;
            this.description = description;
        }

        public PsiClass getPsiClass() {
            return psiClass;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
