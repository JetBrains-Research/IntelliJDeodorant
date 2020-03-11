package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.ui.treeStructure.treetable.TreeTableModel;
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

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof AbstractCandidateRefactoringGroup) {
            AbstractCandidateRefactoringGroup group = (AbstractCandidateRefactoringGroup) parent;
            return group.getCandidates().get(index);
        }
        return candidateRefactoringGroups.get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof AbstractCandidateRefactoringGroup) {
            AbstractCandidateRefactoringGroup group = (AbstractCandidateRefactoringGroup) parent;
            return group.getCandidates().size();
        }

        if (parent instanceof AbstractCandidateRefactoring) {
            return 0;
        }

        return candidateRefactoringGroups.size();
    }

    RefactoringType getRefactoringType() {
        return refactoringType;
    }
}