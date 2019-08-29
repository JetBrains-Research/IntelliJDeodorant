package gr.uom.java.ast;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;

public class ASTInformation {

    private final PsiType iTypeRoot;
    private final int startPosition;
    private final int length;
    private volatile int hashCode = 0;
    private final PsiElement psiElement;

    public ASTInformation(PsiType iTypeRoot, PsiElement astNode) {
        this.iTypeRoot = iTypeRoot;
        if (astNode != null && astNode.getTextRange() != null) {
            this.startPosition = astNode.getTextRange().getStartOffset();
            this.length = astNode.getTextRange().getLength();
        } else if (astNode == null) {
            this.startPosition = 0;
            this.length = 0;
        } else {
            this.startPosition = astNode.getTextOffset();
            this.length = astNode.getTextLength();
        }
        this.psiElement = astNode;
    }

    public PsiElement recoverASTNode() {
        return psiElement;
    }

    PsiType getITypeRoot() {
        return iTypeRoot;
    }

    int getStartPosition() {
        return startPosition;
    }

    int getLength() {
        return length;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof ASTInformation) {
            ASTInformation astInformation = (ASTInformation) o;
            return this.iTypeRoot.equals(astInformation.iTypeRoot) &&
                    this.startPosition == astInformation.startPosition &&
                    this.length == astInformation.length;
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + iTypeRoot.hashCode();
            result = 37 * result + startPosition;
            result = 37 * result + length;
            hashCode = result;
        }
        return hashCode;
    }
}
