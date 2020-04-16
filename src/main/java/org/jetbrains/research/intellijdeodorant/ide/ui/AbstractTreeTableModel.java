package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.sun.istack.Nullable;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringType;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringType.AbstractCandidateRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringType.AbstractCandidateRefactoringGroup;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.List;

public abstract class AbstractTreeTableModel extends DefaultTreeModel implements TreeTableModel {
    private final String[] columnNames;
    protected List<AbstractCandidateRefactoringGroup> candidateRefactoringGroups;
    private final RefactoringType refactoringType;

    public AbstractTreeTableModel(List<AbstractCandidateRefactoringGroup> candidateRefactoringGroups,
                                  String[] columnNames, RefactoringType refactoringType) {
        super(new DefaultMutableTreeNode("root"));
        this.candidateRefactoringGroups = candidateRefactoringGroups;
        this.columnNames = columnNames;
        this.refactoringType = refactoringType;
    }

    public void setCandidateRefactoringGroups(List<AbstractCandidateRefactoringGroup> candidateRefactoringGroups) {
        this.candidateRefactoringGroups = candidateRefactoringGroups;
    }

    public List<AbstractCandidateRefactoringGroup> getCandidateRefactoringGroups() {
        return this.candidateRefactoringGroups;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class getColumnClass(int column) {
        if (column == 0) {
            return TreeTableModel.class;
        }
        return String.class;
    }

    @Override
    public abstract Object getValueAt(Object o, int index);

    @Override
    public boolean isCellEditable(Object node, int column) {
        return false;
    }

    @Override
    public void setValueAt(Object aValue, Object node, int column) {
    }

    @Override
    public void setTree(JTree tree) {
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof AbstractCandidateRefactoring;
    }

    @Nullable
    public List<?> getChildren(Object parent) {
        if (parent instanceof AbstractCandidateRefactoringGroup) {
            AbstractCandidateRefactoringGroup group = (AbstractCandidateRefactoringGroup) parent;
            return group.getCandidates();
        }

        if (parent instanceof AbstractCandidateRefactoring) {
            return null;
        }

        return candidateRefactoringGroups;
    }

    @Override
    public Object getChild(Object parent, int index) {
        List<?> children = getChildren(parent);
        if (children != null) {
            return children.get(index);
        }

        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        List<?> children = getChildren(parent);
        if (children != null) {
            return children.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        List<?> children = getChildren(parent);
        if (children != null) {
            return children.indexOf(child);
        } else {
            return -1;
        }
    }

    RefactoringType getRefactoringType() {
        return refactoringType;
    }
}