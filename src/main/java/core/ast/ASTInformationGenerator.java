package core.ast;

import com.intellij.psi.PsiElement;

public class ASTInformationGenerator {
    
    public static ASTInformation generateASTInformation(PsiElement astNode) {
        return new ASTInformation(astNode);
    }

}
