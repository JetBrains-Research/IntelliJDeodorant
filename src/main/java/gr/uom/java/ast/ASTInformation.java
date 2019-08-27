package gr.uom.java.ast;

import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;


public class ASTInformation {

    private PsiType iTypeRoot;
    private int startPosition;
    private int length;
    //  private PsiType nodeType;
    private volatile int hashCode = 0;
    PsiElement psiElement;

    public ASTInformation(PsiType iTypeRoot, PsiElement astNode) {
        this.iTypeRoot = iTypeRoot;
        if (astNode != null && astNode.getTextRange() != null) {
            this.startPosition = astNode.getTextRange().getStartOffset();
            this.length = astNode.getTextRange().getLength();
        } else if (astNode == null ) {
            this.startPosition = 0;
            this.length = 0;
        } else {
            this.startPosition = astNode.getTextOffset();
            this.length = astNode.getTextLength();
        }
        this.psiElement = astNode;
        //  this.nodeType = astNode.get
    }

    public PsiElement recoverASTNode() {
        // CompilationUnit compilationUnit = CompilationUnitCache.getInstance().getCompilationUnit(iTypeRoot);
        //ASTNode astNode = NodeFinder.perform(compilationUnit, startPosition, length);
        //return astNode;
        return psiElement;
    }

    public PsiType getITypeRoot() {
        return iTypeRoot;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public int getLength() {
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
                    this.length == astInformation.length;// &&
            //  this.nodeType == astInformation.nodeType;
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + iTypeRoot.hashCode();
            result = 37 * result + startPosition;
            result = 37 * result + length;
            //  result = 37 * result + nodeType;
            hashCode = result;
        }
        return hashCode;
    }
}
