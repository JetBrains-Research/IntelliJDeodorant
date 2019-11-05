package ui;

import core.ast.decomposition.cfg.ASTSlice;
import core.ast.decomposition.cfg.ASTSliceGroup;
import utils.IntelliJDeodorantBundle;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.ArrayList;

/**
 * Renders {@link ASTSlice} elements inside JTree.
 */
public class ExtractMethodCandidatesTreeCellRenderer implements TreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        JLabel label = new JLabel();
        label.setIcon(null);
        if (value instanceof ASTSlice) {
            label.setText(value.toString());
        } else if (value instanceof ArrayList) {
            ArrayList list = (ArrayList) value;
            if (!list.isEmpty()) {
                Object element = list.get(0);
                if (element instanceof ASTSlice) {
                    label.setText(element.toString());
                } else if (element instanceof ASTSliceGroup) {
                    label.setText(IntelliJDeodorantBundle.message("extract.method.found.candidates.label"));
                }
            }
        }
        return label;
    }
}
