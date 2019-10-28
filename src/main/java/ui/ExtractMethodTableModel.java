package ui;

import com.intellij.util.ui.tree.AbstractTreeModel;
import core.ast.decomposition.cfg.ASTSliceGroup;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.util.*;

public class ExtractMethodTableModel extends AbstractTreeModel {
    private List<ASTSliceGroup> refactorings;

    ExtractMethodTableModel(List<ASTSliceGroup> refactorings) {
        this.refactorings = refactorings;
    }

    @Override
    public Object getRoot() {
        return refactorings;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof List) {
            List list = (List) parent;
            Object o = list.get(index);
            if (o instanceof ASTSliceGroup) {
                return new ArrayList<>(((ASTSliceGroup) o).getCandidates());
            } else {
                return o;
            }
        } else if (parent instanceof ASTSliceGroup) {
            ASTSliceGroup question = (ASTSliceGroup) parent;
            return new ArrayList<>(question.getCandidates());
        } else return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof List) {
            List list = (List) parent;
            return list.size();
        } else if (parent instanceof ASTSliceGroup) {
            ASTSliceGroup group = (ASTSliceGroup) parent;
            return group.getCandidates().size();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {

    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return 0;
    }

    @Override
    public void addTreeModelListener(@NotNull TreeModelListener l) {

    }

    @Override
    public void removeTreeModelListener(@NotNull TreeModelListener l) {

    }
}