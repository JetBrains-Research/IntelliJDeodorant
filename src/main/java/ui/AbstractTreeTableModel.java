package ui;

import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import ui.abstractrefactorings.AbstractCandidateRefactoringGroup;
import ui.abstractrefactorings.AbstractRefactoring;
import ui.abstractrefactorings.AbstractRefactoringType;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.List;

abstract class AbstractTreeTableModel extends DefaultTreeModel implements TreeTableModel {
    private int numberOfColumns;
    private String[] columnNames;
    private List<AbstractCandidateRefactoringGroup> candidateRefactoringGroups;
    private AbstractRefactoringType refactoringType;

    public AbstractTreeTableModel(List<AbstractCandidateRefactoringGroup> candidateRefactoringGroups, String[] columnNames) {
        super(new DefaultMutableTreeNode("root"));
        this.candidateRefactoringGroups = candidateRefactoringGroups;
        this.columnNames = columnNames;
        this.numberOfColumns = columnNames.length;
    }

    public void setEliminationGroups(List<AbstractCandidateRefactoringGroup> candidateRefactoringGroups) {
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
        return node instanceof AbstractRefactoring;
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
        return candidateRefactoringGroups.size();
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof AbstractCandidateRefactoringGroup) {
            AbstractCandidateRefactoringGroup group = (AbstractCandidateRefactoringGroup) parent;
            AbstractRefactoring refactoring = (AbstractRefactoring) child;
            return group.getCandidates().indexOf(refactoring);
        }
        return candidateRefactoringGroups.indexOf(child);
    }

    AbstractRefactoringType getRefactoringType() {
        return refactoringType;
    }
}