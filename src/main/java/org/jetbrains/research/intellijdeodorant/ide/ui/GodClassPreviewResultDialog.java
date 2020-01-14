package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.DiffRequestFactory;
import com.intellij.diff.actions.impl.MutableDiffRequestChain;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.chains.SimpleDiffRequestChain;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.FileDocumentContentImpl;
import com.intellij.diff.impl.CacheDiffRequestChainProcessor;
import com.intellij.diff.impl.DiffRequestProcessor;
import com.intellij.diff.impl.DiffWindow;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.util.DiffUtil;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.vcs.log.history.FileHistoryDiffPreview;
import org.intellij.lang.annotations.JdkConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.PreviewProcessor;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.RefactoringType;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractclass.ExtractClassRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.ui.listeners.DoubleClickListener;
import org.jetbrains.research.intellijdeodorant.ide.ui.listeners.ElementSelectionListener;
import org.jetbrains.research.intellijdeodorant.ide.ui.listeners.EnterKeyListener;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.Set;

public class GodClassPreviewResultDialog extends DiffWindow {
    private MutableDiffRequestChain myChain;
    private PreviewProcessor previewProcessor;
    private Project project;

    public GodClassPreviewResultDialog(@Nullable Project project, @NotNull MutableDiffRequestChain requestChain, @NotNull DiffDialogHints hints, ExtractClassRefactoring refactoring) {
        super(project, requestChain, hints);
        this.myChain = requestChain;
        this.project = project;

        refactoring.setPreviewUsage();
        WriteCommandAction.runWriteCommandAction(project, refactoring::apply);

        previewProcessor = refactoring.getPreviewProcessor();

        PsiElementFactory factory = PsiElementFactory.getInstance(project);
        PsiFileFactory fileFactory = PsiFileFactory.getInstance(project);

        Set<PsiMethod> keys = previewProcessor.getMethodComparingMap().keySet();
        PsiMethod method2 = previewProcessor.getMethodComparingMap().get(keys.iterator().next());

        PsiFile file1 = fileFactory.createFileFromText(JavaLanguage.INSTANCE, keys.iterator().next().getText());
        PsiFile file2 = fileFactory.createFileFromText(JavaLanguage.INSTANCE, method2.getText());

        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        //DiffRequestFactory requestFactory = DiffRequestFactory.getInstance();

        DiffContent c1 = contentFactory.create(project, file1.getVirtualFile());
        DiffContent c2 = contentFactory.create(project, file2.getVirtualFile());

        myChain.setContent1(c1);
        myChain.setContent2(c2);
    }

    @NotNull
    @Override
    protected DiffRequestProcessor createProcessor() {
        return new MyCacheDiffRequestChainProcessor(myProject, myChain);
    }

    private class MyCacheDiffRequestChainProcessor extends CacheDiffRequestChainProcessor {
        private TreeTableModel model = new TreeTableList();
        private TreeTable treeTable = new TreeTable(model);
        private JScrollPane scrollPane;


        MyCacheDiffRequestChainProcessor(@Nullable Project project, @NotNull DiffRequestChain requestChain) {
            super(project, requestChain);

            JPanel myPanel = new JPanel();
            BorderLayout layout = new BorderLayout();
            myPanel.setLayout(layout);

            createTablePanel();

            layout.addLayoutComponent(scrollPane, BorderLayout.WEST);
            myPanel.add(scrollPane);

            getComponent().add(myPanel, BorderLayout.NORTH);
        }

        @Override
        protected void setWindowTitle(@NotNull String title) {
            getWrapper().setTitle(title);
        }

        @Override
        protected void onAfterNavigate() {
            DiffUtil.closeWindow(getWrapper().getWindow(), true, true);
        }

        private void createTablePanel() {
            treeTable.setRootVisible(false);
            treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            scrollPane = ScrollPaneFactory.createScrollPane(treeTable);
        }

        private class TreeTableList extends DefaultTreeModel implements TreeTableModel {

            private TreeTableList() {
                super(new DefaultMutableTreeNode("root"));
            }

            @Override
            public int getColumnCount() {
                return 1;
            }

            @Override
            public String getColumnName(int column) {
                return "lol";
            }

            @Override
            public Class getColumnClass(int column) {
                if (column == 0) {
                    return TreeTableModel.class;
                }
                return String.class;
            }

            @Override
            public Object getValueAt(Object node, int column) {
                return "kek " + node.toString();
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
            }

            @Override
            public boolean isLeaf(Object node) {
                return false;
            }

            @Override
            public Object getChild(Object parent, int index) {
                if (index < 5) {
                    return "lol " + index;
                } else {
                    return null;
                }
            }

            @Override
            public int getChildCount(Object parent) {
                return 5;
            }
        }
    }
}
