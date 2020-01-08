package org.jetbrains.research.intellijdeodorant.ide.ui.listeners;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

@FunctionalInterface
public interface DoubleClickListener extends MouseListener {
    void onDoubleClick();

    default void mouseClicked(MouseEvent e) {
        if (e.getClickCount() >= 2) {
            onDoubleClick();
        }
    }

    default void mousePressed(MouseEvent e) {
    }

    default void mouseReleased(MouseEvent e) {
    }

    default void mouseEntered(MouseEvent e) {
    }

    default void mouseExited(MouseEvent e) {
    }
}
