package org.jetbrains.research.intellijdeodorant.ide.refactoring;

import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.abstractrefactorings.RefactoringType;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.List;

public abstract class AbstractTreeTableModel extends DefaultTreeModel implements TreeTableModel {
    private int numberOfColumns;
    private String[] columnNames;
    protected List<RefactoringType.AbstractCandidateRefactoringGroup> candidateRefactoringGroups;
    private RefactoringType refactoringType;

    public AbstractTreeTableModel(List<RefactoringType.AbstractCandidateRefactoringGroup> candidateRefactoringGroups, String[] columnNames, RefactoringType refactoringType) {
        super(new DefaultMutableTreeNode("root"));
        this.candidateRefactoringGroups = candidateRefactoringGroups;
        this.columnNames = columnNames;
        this.numberOfColumns = columnNames.length;
        this.refactoringType = refactoringType;
    }

    public void setEliminationGroups(List<RefactoringType.AbstractCandidateRefactoringGroup> candidateRefactoringGroups) {
        this.candidateRefactoringGroups = candidateRefactoringGroups;
    }

    @Override
    public int getColumnCount() {
        return numberOfColumns;
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
        "".hashCode();
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof RefactoringType.AbstractCandidateRefactoring;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof RefactoringType.AbstractCandidateRefactoringGroup) {
            RefactoringType.AbstractCandidateRefactoringGroup group = (RefactoringType.AbstractCandidateRefactoringGroup) parent;
            return group.getCandidates().get(index);
        }
        return candidateRefactoringGroups.get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof RefactoringType.AbstractCandidateRefactoringGroup) {
            RefactoringType.AbstractCandidateRefactoringGroup group = (RefactoringType.AbstractCandidateRefactoringGroup) parent;
            return group.getCandidates().size();
        }

        if (parent instanceof RefactoringType.AbstractCandidateRefactoring) {
            return 0;
        }

        return candidateRefactoringGroups.size();
    }

    /*
    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof RefactoringType.AbstractCandidateRefactoringGroup) {
            RefactoringType.AbstractCandidateRefactoringGroup group = (RefactoringType.AbstractCandidateRefactoringGroup) parent;
            RefactoringType.AbstractCandidateRefactoring candidate = (RefactoringType.AbstractCandidateRefactoring) child;
            return group.getCandidates().indexOf(candidate);
        }
        return candidateRefactoringGroups.indexOf(child);
    }
     */

    RefactoringType getRefactoringType() {
        return refactoringType;
    }
}