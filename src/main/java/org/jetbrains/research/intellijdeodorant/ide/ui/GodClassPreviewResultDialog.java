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

        /*
        TODO
        VirtualFile v1 = refactoring.getSourceFile().getVirtualFile();
            Document document1 = FileDocumentManager.getInstance().getDocument(v1);
            FileDocumentContentImpl fileDocumentContent1 = new FileDocumentContentImpl(getProject(), document1, v1);

            VirtualFile v2 = refactoring.getSourceFile().getVirtualFile();
            Document document2 = FileDocumentManager.getInstance().getDocument(v2);
            FileDocumentContentImpl fileDocumentContent2 = new FileDocumentContentImpl(getProject(), document2, v2);

            SimpleDiffRequest simpleDiffRequest = new SimpleDiffRequest("difffff", fileDocumentContent1, fileDocumentContent2,
                    refactoring.getSourceFile().getName(), refactoring.getSourceFile().getName());

            DiffRequestChain chain = new SimpleDiffRequestChain(simpleDiffRequest);
            GodClassPreviewResultDialog previewResultDialog = new GodClassPreviewResultDialog(getProject(), chain, DiffDialogHints.DEFAULT);
            previewResultDialog.show();

            setPreviewResults(false);
         */
    }
}
