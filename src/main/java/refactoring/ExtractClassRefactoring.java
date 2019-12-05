package refactoring;

import com.intellij.lang.ASTNode;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import core.ast.decomposition.cfg.PlainVariable;
import core.ast.util.ExpressionExtractor;
import core.ast.util.MethodDeclarationUtility;
import core.ast.util.TypeVisitor;
import core.ast.util.math.AdjacencyList;
import core.ast.util.math.Edge;
import core.ast.util.math.Node;
import core.ast.util.math.TarjanAlgorithm;
import org.eclipse.jdt.internal.compiler.ast.MarkerAnnotation;

import java.util.*;

@SuppressWarnings("FieldCanBeLocal")
public class ExtractClassRefactoring {
    private static final String GETTER_PREFIX = "get";
    private static final String SETTER_PREFIX = "set";
    private static final String ACCESSOR_SUFFIX = "2";
    private PsiFile sourceFile;
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

    public ExtractClassRefactoring(PsiFile sourceFile, PsiClass sourceTypeDeclaration,
                                   Set<PsiField> extractedFieldFragments, Set<PsiMethod> extractedMethods, Set<PsiMethod> delegateMethods, String extractedTypeName) {
        this.sourceFile = sourceFile;
        //this.sourceCompilationUnit = sourceCompilationUnit;
        this.sourceTypeDeclaration = sourceTypeDeclaration;
        //this.compilationUnitChanges = new LinkedHashMap<ICompilationUnit, CompilationUnitChange>();
        //ICompilationUnit sourceICompilationUnit = (ICompilationUnit) sourceCompilationUnit.getJavaElement();
        //MultiTextEdit sourceMultiTextEdit = new MultiTextEdit();
        //CompilationUnitChange sourceCompilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
        //sourceCompilationUnitChange.setEdit(sourceMultiTextEdit);
        //this.compilationUnitChanges.put(sourceICompilationUnit, sourceCompilationUnitChange);
        //this.createCompilationUnitChanges = new LinkedHashMap<ICompilationUnit, CreateCompilationUnitChange>();
        //this.javaElementsToOpenInEditor = new LinkedHashSet<IJavaElement>();
        this.requiredImportDeclarationsInExtractedClass = new LinkedHashSet<>();
        this.additionalArgumentsAddedToExtractedMethods = new LinkedHashMap<>();
        this.additionalParametersAddedToExtractedMethods = new LinkedHashMap<>();
        this.sourceMethodBindingsChangedWithPublicModifier = new LinkedHashSet<>();
        this.sourceFieldBindingsWithCreatedSetterMethod = new LinkedHashSet<>();
        this.sourceFieldBindingsWithCreatedGetterMethod = new LinkedHashSet<>();
        this.fieldDeclarationsChangedWithPublicModifier = new LinkedHashSet<>();
        this.memberTypeDeclarationsChangedWithPublicModifier = new LinkedHashSet<>();
        this.oldMethodInvocationsWithinExtractedMethods = new LinkedHashMap<>();
        this.newMethodInvocationsWithinExtractedMethods = new LinkedHashMap<>();
        this.oldToNewExtractedMethodDeclarationMap = new LinkedHashMap<>();
        this.extractedFieldFragments = extractedFieldFragments;
        this.extractedMethods = extractedMethods;
        this.delegateMethods = delegateMethods;
        this.extractedTypeName = extractedTypeName;
        this.leaveDelegateForPublicMethods = false;
        //this.statementRewriteMap = new LinkedHashMap<PsiStatement, ASTRewrite>();
        this.constructorFinalFieldAssignmentMap = new LinkedHashMap<>();
        this.extractedClassConstructorParameterMap = new LinkedHashMap<>();
        this.extractedFieldsWithThisExpressionInTheirInitializer = new LinkedHashSet<>();
        this.staticallyImportedMethods = new LinkedHashSet<>();
        for (PsiMethod extractedMethod : extractedMethods) {
            additionalArgumentsAddedToExtractedMethods.put(extractedMethod, new LinkedHashSet<>());
            additionalParametersAddedToExtractedMethods.put(extractedMethod, new LinkedHashSet<>());
        }

        this.project = sourceFile.getProject();
        this.factory = PsiElementFactory.SERVICE.getInstance(project);
        this.fileFactory = PsiFileFactory.getInstance(project);
        this.psiManager = PsiManager.getInstance(project);
    }

    public String getExtractedTypeName() {
        return extractedTypeName;
    }

    public void setExtractedTypeName(String targetTypeName) {
        this.extractedTypeName = targetTypeName;
    }

    public void setLeaveDelegateForPublicMethods(boolean leaveDelegateForPublicMethods) {
        this.leaveDelegateForPublicMethods = leaveDelegateForPublicMethods;
    }



    public void apply() {
        /*
        for (PsiMethod method : extractedMethods) {
            if (!method.getModifierList().hasModifierProperty(PsiModifier.PRIVATE))
                delegateMethods.add(method);
        }
        removeFieldFragmentsInSourceClass(extractedFieldFragments);
        Set<PsiField> modifiedFieldsInNonExtractedMethods = new LinkedHashSet<>();
        Set<PsiField> accessedFieldsInNonExtractedMethods = new LinkedHashSet<>();
        modifyExtractedFieldAssignmentsInSourceClass(extractedFieldFragments, modifiedFieldsInNonExtractedMethods, accessedFieldsInNonExtractedMethods);
        modifyExtractedFieldAccessesInSourceClass(extractedFieldFragments, accessedFieldsInNonExtractedMethods);
        createExtractedTypeFieldReferenceInSourceClass();

        Set<PsiType> typeBindings = new LinkedHashSet<>();
        TypeVisitor typeVisitor = new TypeVisitor(); //PsiJavaElementVisitor
        for (PsiVariable fieldFragment : extractedFieldFragments) {
            fieldFragment.getParent().accept(typeVisitor); //TODO getParent() should probably be removed OR we should visit class of this field
            typeBindings.addAll(typeVisitor.getTypeBindings());
        }
        for (PsiMethod method : extractedMethods) {
            method.accept(typeVisitor);
            typeBindings.addAll(typeVisitor.getTypeBindings());
        }
        RefactoringUtility.getSimpleTypeBindings(typeBindings, requiredImportDeclarationsInExtractedClass); //TODO PsiType? PsiClass? PsiImportStatementDeclaration?

        createExtractedClass(modifiedFieldsInNonExtractedMethods, accessedFieldsInNonExtractedMethods);
        modifyExtractedMethodInvocationsInSourceClass();
        handleInitializationOfExtractedFieldsWithThisExpressionInTheirInitializer();

        Set<PsiMethod> methodsToBeRemoved = new LinkedHashSet<>();
        for (PsiMethod method : extractedMethods) {
            if (delegateMethods.contains(method))
                addDelegationInExtractedMethod(method);
            else
                methodsToBeRemoved.add(method);
        }
        if (methodsToBeRemoved.size() > 0)
            removeSourceMethods(methodsToBeRemoved);

         */
    }

    public PsiFile getSourceFile() {
        return sourceFile;
    }
}