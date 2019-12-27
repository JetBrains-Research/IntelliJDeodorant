package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.util.ui.tree.AbstractTreeModel;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.ASTSliceGroup;

import javax.swing.tree.TreePath;
import java.util.*;

class ExtractMethodTableModel extends AbstractTreeModel {
    private List<ASTSliceGroup> refactorings;

    public ExtractMethodTableModel(List<ASTSliceGroup> refactorings) {
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
            ASTSliceGroup sliceGroup = (ASTSliceGroup) parent;
            return new ArrayList<>(sliceGroup.getCandidates());
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
}