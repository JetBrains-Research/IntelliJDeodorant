package gr.uom.java.ast;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;

import java.util.List;

public class MethodInvocationObject extends AbstractMethodInvocationObject {

    public MethodInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType) {
        super(originClassType, methodName, returnType);
    }

    public MethodInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType, List<TypeObject> parameterList) {
        super(originClassType, methodName, returnType, parameterList);
    }

    public void setMethodInvocation(PsiMethodCallExpression methodInvocation) {
    	//this.methodInvocation = methodInvocation;
    	this.methodInvocation = ASTInformationGenerator.generateASTInformation(methodInvocation);
    }

    public PsiMethodCallExpression getMethodInvocation() {
    	//return this.methodInvocation;
    	return (PsiMethodCallExpression)this.methodInvocation.recoverASTNode();
    }
}