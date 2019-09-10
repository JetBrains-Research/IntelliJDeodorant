package core.ast;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;

public class ASTInformationGenerator {

    private static PsiType iTypeRoot;

    public static void setCurrentITypeRoot(PsiType typeRoot) {
        iTypeRoot = typeRoot;
    }

    public static ASTInformation generateASTInformation(PsiElement astNode) {
        return new ASTInformation(iTypeRoot, astNode);
    }
}
