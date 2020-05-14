package org.jetbrains.research.intellijdeodorant.ide.ui.listeners;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

@FunctionalInterface
public interface EnterKeyListener extends KeyListener {
    void onEnterKey(InputEvent e);

    default void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            onEnterKey(e);
        }
    }

    default void keyTyped(KeyEvent e) {
    }

    default void keyReleased(KeyEvent e) {
    }
}
