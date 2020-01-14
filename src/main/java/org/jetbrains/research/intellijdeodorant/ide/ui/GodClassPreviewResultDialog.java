package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.actions.impl.MutableDiffRequestChain;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.impl.CacheDiffRequestChainProcessor;
import com.intellij.diff.impl.DiffRequestProcessor;
import com.intellij.diff.impl.DiffWindow;
import com.intellij.diff.util.DiffUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractclass.ExtractClassPreviewProcessor;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractclass.ExtractClassPreviewProcessor.PsiElementPair;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractclass.ExtractClassPreviewProcessor.PsiMethodPair;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractclass.ExtractClassRefactoring;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Set;

public class GodClassPreviewResultDialog extends DiffWindow {
    private MutableDiffRequestChain myChain;
    private ExtractClassPreviewProcessor previewProcessor;
    private Project project;

    public GodClassPreviewResultDialog(@Nullable Project project, @NotNull MutableDiffRequestChain requestChain, @NotNull DiffDialogHints hints, ExtractClassRefactoring refactoring) {
        super(project, requestChain, hints);
        this.myChain = requestChain;
        this.project = project;

        refactoring.setPreviewUsage();
        WriteCommandAction.runWriteCommandAction(project, refactoring::apply);

        previewProcessor = refactoring.getPreviewProcessor();

        PsiMethod method1 = previewProcessor.getMethodComparingList().get(5).getInitialPsiMethod();
        PsiMethod method2 = previewProcessor.getMethodComparingList().get(5).getUpdatedPsiMethod();

        DiffContentFactory contentFactory = DiffContentFactory.getInstance();

        DiffContent c1 = contentFactory.create(project, method1.getText(), JavaFileType.INSTANCE);
        DiffContent c2 = contentFactory.create(project, method2.getText(), JavaFileType.INSTANCE);

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
                return "";
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
                if (node instanceof PsiMethodPair) {
                    return ((PsiMethodPair) node).getInitialPsiMethod().getName();
                } else if (node instanceof PsiElementPair) {
                    return ((PsiElementPair) node).getInitialPsiElement().getText();
                }

                return "";
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
                return node instanceof PsiElementPair;
            }

            @Override
            public Object getChild(Object parent, int index) {
                if (parent instanceof PsiMethodPair) {
                    PsiMethod initialMethod = ((PsiMethodPair) parent).getInitialPsiMethod();
                    return previewProcessor.getMethodElementsComparingMap().get(initialMethod);
                }

                return previewProcessor.getMethodComparingList().get(index);
            }

            @Override
            public int getChildCount(Object parent) {
                if (parent instanceof PsiMethodPair) {
                    PsiMethod initialMethod = ((PsiMethodPair) parent).getInitialPsiMethod();
                    return previewProcessor.getMethodElementsComparingMap().get(initialMethod).size();
                }

                return previewProcessor.getMethodComparingList().size();
            }
        }
    }
}
