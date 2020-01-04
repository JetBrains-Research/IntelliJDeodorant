package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.actions.CompareFilesAction;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.impl.DiffWindow;
import com.intellij.diff.impl.DiffWindowBase;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GodClassPreviewResultDialog extends DiffWindow {

    public GodClassPreviewResultDialog(@Nullable Project project, @NotNull DiffRequestChain requestChain, @NotNull DiffDialogHints hints) {
        super(project, requestChain, hints);
    }
}
