package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.impl.DiffWindow;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GodClassPreviewResultDialog extends DiffWindow {

    public GodClassPreviewResultDialog(@Nullable Project project, @NotNull DiffRequestChain requestChain, @NotNull DiffDialogHints hints) {
        super(project, requestChain, hints);
    }
}
