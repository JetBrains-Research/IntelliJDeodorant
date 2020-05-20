package org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass;

import com.intellij.psi.*;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;

import java.util.*;

/**
 * Collects the changes during the Extract Class Refactoring in order to show them later.
 */
public class ExtractClassPreviewProcessor {
    /**
     * Stores initial and updated {@link PsiMethod} insanities (to compare).
     */
    private List<PsiMethodComparingPair> methodComparingList = new ArrayList<>();

    /**
     * For each updated {@link PsiMethod}, stores all changes applied to the method or null if there are no changes.
     */
    private Map<PsiMethod, ArrayList<PsiElementComparingPair>> methodElementsComparingMap = new LinkedHashMap<>();

    /**
     * Stores class-level changes (field adding/removing, etc).
     */
    private List<PsiElementChange> psiElementChanges = new ArrayList<>();

    /**
     * Copy of the source class at it's initial state.
     */
    private PsiClassWrapper initialSourceClass;

    /**
     * Source class after refactoring.
     */
    private PsiClassWrapper updatedSourceClass;
    private PsiClassWrapper extractedClass;

    private Map<PsiElement, PsiElement> updatedSourceElementsToInitial = new HashMap<>();

    /**
     * Creates a copy of the source file (of the real project file) in order to make all changes to the copy file.
     */
    public CopiedData cloneSourceFile(PsiJavaFile sourceFile, PsiClass sourceTypeDeclaration,
                                      Set<PsiField> extractedFieldFragments, Set<PsiMethod> extractedMethods,
                                      Set<PsiMethod> delegateMethods) {
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

    /**
     * Removes a method comparision if there are no changes.
     */
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

    /**
     * Adds to `psiElementChanges` new change that was initialised with the elements from the updated class,
     * as `psiElementChanges` should store elements of the initial-copy class.
     */
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

    /**
     * Sorts all changes by their offset.
     */
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
        /**
         * Description to show in the preview dialog.
         */
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

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Change that was applied to some element in the source class, at a class level (added/removed field etc).
     */
    public static class PsiElementChange {
        private PsiElement psiElement;
        private ChangeType changeType;
        /**
         * Description to show in the preview result dialog.
         */
        private String description;

        /**
         * For ADD_AFTER, ADD_BEFORE -- element to add after/before
         * For REPLACE -- element to replace with
         * For REMOVE -- null
         */
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

        public ChangeType getChangeType() {
            return changeType;
        }

        public enum ChangeType {
            REMOVE,
            ADD_AFTER,
            ADD_BEFORE,
            REPLACE
        }

        public PsiElementChange(PsiElement psiElement, ChangeType changeType, String description) {
            this(psiElement, changeType, description, null);
        }

        public PsiElementChange(PsiElement psiElement, ChangeType changeType, String description, PsiElement anchor) {
            this.psiElement = psiElement;
            this.changeType = changeType;
            this.description = description;
            this.anchor = anchor;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Wrapper to correctly show description in the preview result dialog.
     */
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
