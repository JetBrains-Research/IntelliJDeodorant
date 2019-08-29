package ui;

import navigation.Reference;

import javax.swing.*;
import java.awt.*;

public class ReferenceListCellRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        Reference reference = (Reference) value;
        setText(reference.description());
        setIcon(reference.icon());
        return this;
    }
}