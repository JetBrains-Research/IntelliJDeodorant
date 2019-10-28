package core.ast;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class FieldObject extends VariableDeclarationObject {

    private final String name;
    private final TypeObject type;
    private final List<CommentObject> commentList;
    private boolean _static;
    private Access access;
    private String className;
    private PsiField psiField;
    private volatile int hashCode = 0;

    public FieldObject(TypeObject type, String fieldName, PsiField field) {
        this.type = type;
        this.name = fieldName;
        this._static = false;
        this.access = Access.NONE;
        this.commentList = new ArrayList<>();
        this.psiField = field;
    }

    private PsiVariable getVariableDeclarationFragment() {
        return psiField;
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    public Access getAccess() {
        return access;
    }

    public String getName() {
        return name;
    }

    public TypeObject getType() {
        return type;
    }

    public boolean addComment(CommentObject comment) {
        return commentList.add(comment);
    }

    public void addComments(List<CommentObject> comments) {
        commentList.addAll(comments);
    }

    public ListIterator<CommentObject> getCommentListIterator() {
        return commentList.listIterator();
    }

    public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof FieldObject) {
            FieldObject fieldObject = (FieldObject) o;
            return this.psiField.equals(fieldObject.psiField);
        }
        return false;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return this.className;
    }

    public boolean equals(FieldInstructionObject fio) {
        return this.className.equals(fio.getOwnerClass())
                && this.name.equals(fio.getName())
                && this.type.equals(fio.getType());
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + className.hashCode();
            result = 37 * result + name.hashCode();
            result = 37 * result + type.hashCode();
            result = 37 * result + psiField.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!access.equals(Access.NONE))
            sb.append(access.toString()).append(" ");
        if (_static)
            sb.append("static").append(" ");
        sb.append(type.toString()).append(" ");
        sb.append(name);
        return sb.toString();
    }

    public PsiVariable getVariableDeclaration() {
        return getVariableDeclarationFragment();
    }
}