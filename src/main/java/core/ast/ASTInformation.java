package core.ast;

import com.intellij.psi.PsiElement;

public class ASTInformation {

    private final int startPosition;
    private final int length;
    private volatile int hashCode = 0;
    private final PsiElement psiElement;

    public ASTInformation(PsiElement astNode) {
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
            return this.startPosition == astInformation.startPosition
                    && this.length == astInformation.length;
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + startPosition;
            result = 37 * result + length;
            hashCode = result;
        }
        return hashCode;
    }
}
