package gr.uom.java.ast;

import com.intellij.psi.PsiSuperExpression;

import java.util.List;

public class SuperMethodInvocationObject extends AbstractMethodInvocationObject {

    public SuperMethodInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType) {
        super(originClassType, methodName, returnType);
    }

    public SuperMethodInvocationObject(TypeObject originClassType, String methodName, TypeObject returnType, List<TypeObject> parameterList) {
        super(originClassType, methodName, returnType, parameterList);
    }

    public void setSuperMethodInvocation(PsiSuperExpression superMethodInvocation) {
        this.methodInvocation = ASTInformationGenerator.generateASTInformation(superMethodInvocation);
    }

    public PsiSuperExpression getSuperMethodInvocation() {
        return (PsiSuperExpression) this.methodInvocation.recoverASTNode();
    }
}
