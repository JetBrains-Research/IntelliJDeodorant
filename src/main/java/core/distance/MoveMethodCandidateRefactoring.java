package core.distance;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import core.FeatureEnvyVisualizationData;
import core.ast.FieldInstructionObject;
import core.ast.MethodObject;
import core.ast.TypeObject;
import core.ast.decomposition.cfg.PlainVariable;
import core.ast.MethodInvocationObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static utils.PsiUtils.isTestClass;

public class MoveMethodCandidateRefactoring extends CandidateRefactoring implements Comparable<MoveMethodCandidateRefactoring> {
    private final MySystem system;
    private final MyClass sourceClass;
    private final MyClass targetClass;
    private final MyMethod sourceMethod;
    //contains source class methods that do not access any field or method and are accessed only by sourceMethod
    private final Map<PsiMethodCallExpression, PsiMethod> additionalMethodsToBeMoved;
    private String movedMethodName;
    private final FeatureEnvyVisualizationData visualizationData;
    private Integer userRate;

    public MoveMethodCandidateRefactoring(MySystem system, MyClass sourceClass, MyClass targetClass, MyMethod sourceMethod) {
        this.system = system;
        this.sourceClass = sourceClass;
        this.targetClass = targetClass;
        this.sourceMethod = sourceMethod;
        this.additionalMethodsToBeMoved = new LinkedHashMap<>();
        this.movedMethodName = sourceMethod.getMethodName();
        List<MethodInvocationObject> methodInvocations = sourceMethod.getMethodObject().getMethodInvocations();
        for (MethodInvocationObject methodInvocation : methodInvocations) {
            if (methodInvocation.getOriginClassName().equals(sourceClass.getClassObject().getName()) &&
                    !sourceClass.getClassObject().containsMethodInvocation(methodInvocation, sourceMethod.getMethodObject()) &&
                    !system.getSystemObject().containsMethodInvocation(methodInvocation, sourceClass.getClassObject())) {
                MethodObject invokedMethod = sourceClass.getClassObject().getMethod(methodInvocation);
                boolean systemMemberAccessed = false;
                if (invokedMethod != null) {
                    for (MethodInvocationObject methodInvocationObject : invokedMethod.getMethodInvocations()) {
                        if (system.getSystemObject().getClassObject(methodInvocationObject.getOriginClassName()) != null) {
                            systemMemberAccessed = true;
                            break;
                        }
                    }
                }
                if (!systemMemberAccessed && invokedMethod != null) {
                    for (FieldInstructionObject fieldInstructionObject : invokedMethod.getFieldInstructions()) {
                        if (system.getSystemObject().getClassObject(fieldInstructionObject.getOwnerClass()) != null) {
                            systemMemberAccessed = true;
                            break;
                        }
                    }
                }
                if (invokedMethod != null && !systemMemberAccessed && !additionalMethodsToBeMoved.containsKey(methodInvocation.getMethodInvocation()))
                    additionalMethodsToBeMoved.put(methodInvocation.getMethodInvocation(), invokedMethod.getMethodDeclaration());
            }
        }
        this.visualizationData = new FeatureEnvyVisualizationData(sourceClass.getClassObject(),
                sourceMethod.getMethodObject(), targetClass.getClassObject());
    }

    boolean isApplicable() {
        return !isSynchronized() && !containsSuperMethodInvocation() && !overridesMethod() && !containsFieldAssignment()
                && !isTargetClassAnInterface() && validTargetObject() && !oneToManyRelationshipWithTargetClass()
                && !containsAssignmentToTargetClassVariable() && !containsMethodCallWithThisExpressionAsArgument()
                && !isTargetClassAnEnum() && !isSourceClassATestClass()
                && !targetClassContainsMethodWithSourceMethodSignature() && !containsNullCheckForTargetObject();
    }

    public boolean leaveDelegate() {
        return system.getSystemObject().containsMethodInvocation(getSourceMethod().getMethodObject().generateMethodInvocation(), getSourceClass().getClassObject()) ||
                system.getSystemObject().containsSuperMethodInvocation(getSourceMethod().getMethodObject().generateSuperMethodInvocation());
    }

    private boolean targetClassContainsMethodWithSourceMethodSignature() {
        MethodObject sourceMethod = this.sourceMethod.getMethodObject();
        for (MethodObject targetMethod : targetClass.getClassObject().getMethodList()) {
            if (targetMethod.getName().equals(sourceMethod.getName()) &&
                    targetMethod.getReturnType().equals(sourceMethod.getReturnType())) {
                if (targetMethod.getParameterTypeList().equals(sourceMethod.getParameterTypeList())) {
                    return true;
                } else {
                    List<TypeObject> sourceParameterTypeListWithoutTargetType = new ArrayList<>();
                    for (TypeObject type : sourceMethod.getParameterTypeList()) {
                        if (!type.getClassType().equals(targetClass.getName())) {
                            sourceParameterTypeListWithoutTargetType.add(type);
                        }
                    }
                    if (targetMethod.getParameterTypeList().equals(sourceParameterTypeListWithoutTargetType)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isSourceClassATestClass() {
        return isTestClass(sourceClass.getClassObject().getPsiClass())
                || sourceClass.getClassObject().containsMethodWithTestAnnotation();
    }

    private boolean isTargetClassAnInterface() {
        return targetClass.getClassObject().isInterface();
    }

    private boolean isTargetClassAnEnum() {
        return targetClass.getClassObject().isEnum();
    }

    private boolean validTargetObject() {
        return sourceMethod.getMethodObject().validTargetObject(sourceClass.getClassObject(), targetClass.getClassObject());
    }

    private boolean oneToManyRelationshipWithTargetClass() {
        return sourceMethod.getMethodObject().oneToManyRelationshipWithTargetClass(system.getAssociationsOfClass(sourceClass.getClassObject()), targetClass.getClassObject());
    }

    private boolean overridesMethod() {
        return sourceMethod.getMethodObject().overridesMethod();
    }

    private boolean containsFieldAssignment() {
        return !sourceMethod.getMethodObject().getDefinedFieldsThroughThisReference().isEmpty();
    }

    private boolean containsAssignmentToTargetClassVariable() {
        Set<PlainVariable> definedVariables = sourceMethod.getMethodObject().getDefinedLocalVariables();
        for (PlainVariable variable : definedVariables) {
            if (variable.isParameter() && variable.getType().equals(targetClass.getName()))
                return true;
        }
        return false;
    }

    private boolean containsSuperMethodInvocation() {
        return sourceMethod.getMethodObject().containsSuperMethodInvocation() || sourceMethod.getMethodObject().containsSuperFieldAccess();
    }

    private boolean isSynchronized() {
        return sourceMethod.getMethodObject().isSynchronized();
    }

    private boolean containsMethodCallWithThisExpressionAsArgument() {
        return sourceMethod.getMethodObject().containsMethodCallWithThisExpressionAsArgument();
    }

    private boolean containsNullCheckForTargetObject() {
        return sourceMethod.getMethodObject().containsNullCheckForTargetObject(targetClass.getClassObject());
    }

    public PsiClass getSourceClassTypeDeclaration() {
        return (PsiClass) sourceClass.getClassObject().getAbstractTypeDeclaration();
    }

    public PsiClass getTargetClassTypeDeclaration() {
        return (PsiClass) targetClass.getClassObject().getAbstractTypeDeclaration();
    }

    public PsiMethod getSourceMethodDeclaration() {
        return sourceMethod.getMethodObject().getMethodDeclaration();
    }

    private MyClass getSourceClass() {
        return sourceClass;
    }

    public MyClass getTargetClass() {
        return targetClass;
    }

    private MyMethod getSourceMethod() {
        return sourceMethod;
    }

    public Map<PsiMethodCallExpression, PsiMethod> getAdditionalMethodsToBeMoved() {
        return additionalMethodsToBeMoved;
    }

    public String getMovedMethodName() {
        return movedMethodName;
    }

    public void setMovedMethodName(String movedMethodName) {
        this.movedMethodName = movedMethodName;
    }

    public String toString() {
        return getSourceEntity() + "->" + getTarget();
    }

    public String getSourceEntity() {
        StringBuilder sb = new StringBuilder();
        sb.append(sourceMethod.getClassOrigin()).append("::");
        sb.append(movedMethodName);
        List<String> parameterList = sourceMethod.getParameterList();
        sb.append("(");
        if (!parameterList.isEmpty()) {
            for (int i = 0; i < parameterList.size() - 1; i++)
                sb.append(parameterList.get(i)).append(", ");
            sb.append(parameterList.get(parameterList.size() - 1));
        }
        sb.append(")");
        if (sourceMethod.getReturnType() != null)
            sb.append(":").append(sourceMethod.getReturnType());
        return sb.toString();
    }

    public String getSource() {
        return sourceClass.getName();
    }

    public String getTarget() {
        return targetClass.getName();
    }

    protected Set<String> getEntitySet() {
        return sourceMethod.getEntitySet();
    }

    private FeatureEnvyVisualizationData getFeatureEnvyVisualizationData() {
        return visualizationData;
    }


    public int getNumberOfDistinctEnviedElements() {
        int counter = 0;
        for (String entity : getEntitySet()) {
            String[] tokens = entity.split("::");
            String classOrigin = tokens[0];
            if (classOrigin.equals(targetClass.getName()))
                counter++;
        }
        return counter;
    }

    public Integer getUserRate() {
        return userRate;
    }

    public void setUserRate(Integer userRate) {
        this.userRate = userRate;
    }

    public int compareTo(MoveMethodCandidateRefactoring other) {
        int thisSourceClassDependencies = this.getDistinctSourceDependencies();
        int otherSourceClassDependencies = other.getDistinctSourceDependencies();
        if (thisSourceClassDependencies != otherSourceClassDependencies) {
            return Integer.compare(thisSourceClassDependencies, otherSourceClassDependencies);
        } else {
            int thisTargetClassDependencies = this.getDistinctTargetDependencies();
            int otherTargetClassDependencies = other.getDistinctTargetDependencies();
            if (thisTargetClassDependencies != otherTargetClassDependencies) {
                return -Integer.compare(thisTargetClassDependencies, otherTargetClassDependencies);
            } else {
                return this.sourceClass.getName().compareTo(other.sourceClass.getName());
            }
        }
    }

    public int getDistinctSourceDependencies() {
        return getFeatureEnvyVisualizationData().getDistinctSourceDependencies();
    }

    public int getDistinctTargetDependencies() {
        return getFeatureEnvyVisualizationData().getDistinctTargetDependencies();
    }
    
    public FeatureEnvyVisualizationData getVisualizationData() {
        return visualizationData;
    }
}
