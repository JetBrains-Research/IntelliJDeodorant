package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiVariable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.ASTSlice;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractMethod.ExtractMethodCandidateGroup;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.List;

public class ExtractMethodTreeTableModel extends DefaultTreeModel implements TreeTableModel {
    protected List<ExtractMethodCandidateGroup> candidateRefactoringGroups = new ArrayList<>();
    private final String[] columnNames = new String[]{
            IntelliJDeodorantBundle.message("long.method.panel.source.method"),
            IntelliJDeodorantBundle.message("long.method.panel.variable.criterion"),
            IntelliJDeodorantBundle.message("long.method.panel.block.based.region"),
            IntelliJDeodorantBundle.message("long.method.panel.duplicated.extracted")
    };

    public ExtractMethodTreeTableModel() {
        super(new DefaultMutableTreeNode(null));
    }

    public void setCandidateRefactoringGroups(List<ExtractMethodCandidateGroup> candidateRefactoringGroups) {
        this.candidateRefactoringGroups = candidateRefactoringGroups;
        reload();
    }

    public List<ExtractMethodCandidateGroup> getCandidateRefactoringGroups() {
        return candidateRefactoringGroups;
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof ASTSlice;
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
    public Object getValueAt(Object o, int index) {
        if (o instanceof ASTSlice) {
            ASTSlice entry = (ASTSlice) o;
            if (entry.getLocalVariableCriterion() == null) return "";
            switch (index) {
                case 1:
                    return entry.getLocalVariableCriterion().getName();
                case 2:
                    return "B" + entry.getBoundaryBlock().getId();
                case 3:
                    int numberOfSliceStatements = entry.getNumberOfSliceStatements();
                    int numberOfDuplicatedStatements = entry.getNumberOfDuplicatedStatements();
                    return numberOfDuplicatedStatements + "/" + numberOfSliceStatements;
                default:
                    return "";
            }
        } else if (o instanceof ExtractMethodCandidateGroup) {
            ExtractMethodCandidateGroup group = (ExtractMethodCandidateGroup) o;
            switch (index) {
                case 0:
                    PsiClass psiClass = group.getMethod().getContainingClass();
                    String declaringClassName = psiClass == null ? "" : psiClass.getQualifiedName();
                    String methodName = group.getMethod().getName();
                    return declaringClassName + "::" + methodName;
                case 1:
                    PsiVariable firstCandidate = group.getCandidates().iterator().next().getLocalVariableCriterion();
                    return firstCandidate == null ? "" : firstCandidate.getName();
                default:
                    return "";
            }
        }
        return "";
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof ExtractMethodCandidateGroup) {
            ExtractMethodCandidateGroup group = (ExtractMethodCandidateGroup) parent;
            return group.getCandidates().size();
        } else if (parent instanceof ASTSlice) {
            return 0;
        } else {
            return candidateRefactoringGroups.size();
        }
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof ExtractMethodCandidateGroup) {
            ExtractMethodCandidateGroup group = (ExtractMethodCandidateGroup) parent;
            ArrayList<ASTSlice> slices = new ArrayList<>(group.getCandidates());
            return slices.get(index);
        } else {
            return candidateRefactoringGroups.get(index);
        }
    }

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

}
