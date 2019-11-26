package ui;

import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import refactoring.TypeCheckElimination;
import refactoring.TypeCheckEliminationGroup;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.List;

class TypeCheckingTreeTableModel extends DefaultTreeModel implements TreeTableModel {
    private final static String[] COLUMN_NAMES = {
            "Type Checking Method",
            "Refactoring Type",
            "System-Level Occurrences",
            "Class-Level Occurrences",
            "Average #statements per case"
    };

    private List<TypeCheckEliminationGroup> eliminationGroups;

    public TypeCheckingTreeTableModel(List<TypeCheckEliminationGroup> eliminationGroups) {
        super(new DefaultMutableTreeNode("root"));
        this.eliminationGroups = eliminationGroups;
    }

    public void setEliminationGroups(List<TypeCheckEliminationGroup> eliminationGroups) {
        this.eliminationGroups = eliminationGroups;
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }


    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
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
        if (o instanceof TypeCheckEliminationGroup) {
            TypeCheckEliminationGroup group = (TypeCheckEliminationGroup)o;
            switch(index){
                case 0:
                    return group.toString();
                case 2:
                    return Integer.toString(group.getGroupSizeAtSystemLevel());
                case 3:
                    return Double.toString(group.getAverageGroupSizeAtClassLevel());
                case 4:
                    return Double.toString(group.getAverageNumberOfStatementsInGroup());
            }
        }
        if (o instanceof TypeCheckElimination) {
            TypeCheckElimination typeCheckElimination = (TypeCheckElimination)o;
            switch(index) {
                case 0:
                    return typeCheckElimination.toString();
                case 1:
                    if(typeCheckElimination.getExistingInheritanceTree() == null) {
                        return "Replace Type Code with State/Strategy";
                    }
                    return "Replace Conditional with Polymorphism";
                case 3:
                    return Integer.toString(typeCheckElimination.getGroupSizeAtClassLevel());
                case 4:
                    return Double.toString(typeCheckElimination.getAverageNumberOfStatements());
            }
        }
        return "";
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
        "".hashCode();
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof TypeCheckElimination;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof TypeCheckEliminationGroup) {
            TypeCheckEliminationGroup group = (TypeCheckEliminationGroup) parent;
            return group.getCandidates().get(index);
        }
        return eliminationGroups.get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof TypeCheckEliminationGroup) {
            TypeCheckEliminationGroup group = (TypeCheckEliminationGroup) parent;
            return group.getCandidates().size();
        }
        return eliminationGroups.size();
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof TypeCheckEliminationGroup) {
            TypeCheckEliminationGroup group = (TypeCheckEliminationGroup) parent;
            TypeCheckElimination elimination = (TypeCheckElimination) child;
            return group.getCandidates().indexOf(elimination);
        }
        return eliminationGroups.indexOf(child);
    }
}