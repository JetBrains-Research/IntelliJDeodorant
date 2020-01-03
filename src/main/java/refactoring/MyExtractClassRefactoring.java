package refactoring;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import core.ast.decomposition.cfg.PlainVariable;
import core.ast.util.ExpressionExtractor;

import javax.swing.*;
import java.text.MessageFormat;
import java.util.*;

@SuppressWarnings("FieldCanBeLocal")
public class MyExtractClassRefactoring {
    private static final String GETTER_PREFIX = "get";
    private static final String SETTER_PREFIX = "set";
    private static final String ACCESSOR_SUFFIX = "2";
    private PsiJavaFile sourceFile;
    private PsiClass sourceTypeDeclaration;
    private Set<PsiElement> javaElementsToOpenInEditor;
    private Set<PsiClass> requiredImportDeclarationsInExtractedClass;
    private Map<PsiMethod, Set<PlainVariable>> additionalArgumentsAddedToExtractedMethods;
    private Map<PsiMethod, Set<PsiParameter>> additionalParametersAddedToExtractedMethods;
    private Set<String> sourceMethodBindingsChangedWithPublicModifier;
    private Set<String> sourceFieldBindingsWithCreatedSetterMethod;
    private Set<String> sourceFieldBindingsWithCreatedGetterMethod;
    private Set<PsiField> fieldDeclarationsChangedWithPublicModifier;
    private Set<?> memberTypeDeclarationsChangedWithPublicModifier;
    private Map<PsiMethod, Set<PsiMethodCallExpression>> oldMethodInvocationsWithinExtractedMethods;
    private Map<PsiMethod, Set<PsiMethodCallExpression>> newMethodInvocationsWithinExtractedMethods;
    private Map<PsiMethod, PsiMethod> oldToNewExtractedMethodDeclarationMap;
    private Set<PsiField> extractedFieldFragments;
    private Set<PsiMethod> extractedMethods;
    private Set<PsiMethod> delegateMethods;
    private String extractedTypeName;
    private boolean leaveDelegateForPublicMethods;
    //this map holds for each constructor the assignment statements that initialize final extracted fields
    private Map<PsiMethod, Map<PsiField, PsiAssignmentExpression>> constructorFinalFieldAssignmentMap;
    //this map hold the parameters that should be passed in each constructor of the extracted class
    private Map<PsiMethod, Set<PsiParameter>> extractedClassConstructorParameterMap;
    private Set<PsiField> extractedFieldsWithThisExpressionInTheirInitializer;
    private Set<PsiMethod> staticallyImportedMethods;

    // TODO comments
    private PsiElementFactory factory;
    private PsiFileFactory fileFactory;
    private Project project;
    private PsiManager psiManager;


}