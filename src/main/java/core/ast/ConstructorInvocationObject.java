package core.ast;

import com.intellij.psi.PsiConstructorCall;

import java.util.List;

public class ConstructorInvocationObject extends AbstractMethodInvocationObject {

    public ConstructorInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType) {
        super(originClassType, methodName, returnType);
    }

    public ConstructorInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType, List<TypeObject> parameterList) {
        super(originClassType, methodName, returnType, parameterList);
    }

    public void setConstructorInvocation(PsiConstructorCall constructorInvocation) {
        this.methodInvocation = ASTInformationGenerator.generateASTInformation(constructorInvocation);
    }

    public PsiConstructorCall getConstructorInvocation() {
        return (PsiConstructorCall) this.methodInvocation.recoverASTNode();
    }
}
