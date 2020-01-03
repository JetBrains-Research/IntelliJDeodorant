package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.ui.JBColor;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.ASTSlice;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.ASTSliceGroup;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;

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
            ASTSlice slice = (ASTSlice) value;
            label.setText(slice.toString());
            disableNotValidSuggestion(slice, label);
        } else if (value instanceof ArrayList) {
            ArrayList list = (ArrayList) value;
            if (!list.isEmpty()) {
                Object element = list.get(0);
                if (element instanceof ASTSlice) {
                    ASTSlice slice = (ASTSlice) element;
                    label.setText(slice.toString());
                    disableNotValidSuggestion(slice, label);
                } else if (element instanceof ASTSliceGroup) {
                    label.setText(IntelliJDeodorantBundle.message("extract.method.found.candidates.label"));
                }
            }
        }
        return label;
    }

    /**
     * Disables not valid suggestions (i.e. already extracted statements).
     *
     * @param slice computation slice.
     * @param label the label to be displayed on tree.
     */
    private void disableNotValidSuggestion(ASTSlice slice, JLabel label) {
        if (!slice.areSliceStatementsValid()) {
            label.setEnabled(false);
            label.setBackground(JBColor.LIGHT_GRAY);
            label.setOpaque(true);
        }
    }
}
