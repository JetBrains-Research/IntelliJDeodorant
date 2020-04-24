package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;

class CommentObject {
    private SmartPsiElementPointer<PsiElement> comment;
    private final String text;
    private final CommentType type;
    private final int startLine;
    private final int endLine;
    private volatile int hashCode = 0;

    public CommentObject(String text, CommentType type, int startLine, int endLine) {
        this.text = text;
        this.type = type;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public PsiComment getComment() {
        return (PsiComment) comment.getElement();
    }

    public String getText() {
        return text;
    }

    public CommentType getType() {
        return type;
    }

    public int getStartPosition() {
        return getComment().getStartOffsetInParent();
    }

    public int getLength() {
        return getComment().getTextLength();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof CommentObject) {
            CommentObject comment = (CommentObject) o;

            return this.getStartPosition() == comment.getStartPosition() &&
                    this.getLength() == comment.getLength();
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + this.getStartPosition();
            result = 37 * result + this.getLength();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        return "Start line: " + startLine + "\n" +
                "End line:" + endLine + "\n" +
                text;
    }
}
