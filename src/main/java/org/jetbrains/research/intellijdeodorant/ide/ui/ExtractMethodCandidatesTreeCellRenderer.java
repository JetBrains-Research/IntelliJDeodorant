package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.ui.JBColor;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.ASTSlice;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractMethod.ExtractMethodCandidateGroup;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * Renders {@link ASTSlice} elements inside JTree.
 */
public class ExtractMethodCandidatesTreeCellRenderer implements TreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        JLabel label = new JLabel();
        label.setIcon(null);
        if (value instanceof ASTSlice) {
            ASTSlice slice = (ASTSlice) value;
            label.setText(slice.toString());
            if (!slice.areSliceStatementsValid()) {
                disableNotValidSuggestion(label);
            }
        } else if (value instanceof ExtractMethodCandidateGroup) {
            ExtractMethodCandidateGroup slice = (ExtractMethodCandidateGroup) value;
            label.setText(slice.toString());
            if (slice.getCandidates().stream().noneMatch(ASTSlice::areSliceStatementsValid)) {
                disableNotValidSuggestion(label);
            }
        }
        return label;
    }

    /**
     * Disables not valid suggestions (i.e. already extracted statements).
     *
     * @param label the label to be displayed on tree.
     */
    private void disableNotValidSuggestion(JLabel label) {
        label.setEnabled(false);
        label.setBackground(JBColor.gray);
        label.setOpaque(true);
    }
}