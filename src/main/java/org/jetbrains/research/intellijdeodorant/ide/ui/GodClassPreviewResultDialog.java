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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractclass.ExtractClassPreviewProcessor;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractclass.ExtractClassPreviewProcessor.PsiElementPair;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractclass.ExtractClassPreviewProcessor.PsiMethodPair;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractclass.ExtractClassRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.ui.listeners.ElementSelectionListener;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;

public class GodClassPreviewResultDialog extends DiffWindow {
    private MutableDiffRequestChain myChain;
    private ExtractClassPreviewProcessor previewProcessor;
    private Project project;
    private PsiElementFactory factory;

    public GodClassPreviewResultDialog(@NotNull Project project, @NotNull MutableDiffRequestChain requestChain, @NotNull DiffDialogHints hints, ExtractClassRefactoring refactoring) {
        super(project, requestChain, hints);
        this.myChain = requestChain;
        this.project = project;
        factory = PsiElementFactory.getInstance(project);

        refactoring.setPreviewUsage();
        WriteCommandAction.runWriteCommandAction(project, refactoring::apply);

        previewProcessor = refactoring.getPreviewProcessor();
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

        private PsiElement lastReplacedElement;
        private PsiElement lastElementThatReplace;
        private PsiElementPair lastElementPair;

        MyCacheDiffRequestChainProcessor(@Nullable Project project, @NotNull DiffRequestChain requestChain) {
            super(project, requestChain);
            JPanel myPanel = new JPanel();
            BorderLayout layout = new BorderLayout();
            myPanel.setLayout(layout);
            createTablePanel();
            layout.addLayoutComponent(scrollPane, BorderLayout.WEST);
            myPanel.add(scrollPane);
            getComponent().add(myPanel, BorderLayout.NORTH);

            JBSplitter splitter = new JBSplitter(true, "DiffRequestProcessor.BottomComponentSplitter", 0.5f);
            splitter.setFirstComponent(myPanel);
            JPanel initialDiffPanel = (JPanel) getComponent().getComponent(0);
            splitter.setSecondComponent(initialDiffPanel);
            getComponent().add(splitter, BorderLayout.CENTER);
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
            treeTable.getTree().addTreeSelectionListener((ElementSelectionListener) this::updateDiff);
            scrollPane = ScrollPaneFactory.createScrollPane(treeTable);
        }

        private void updateDiff() {
            TreePath selectedPath = treeTable.getTree().getSelectionModel().getSelectionPath();

            DiffContentFactory contentFactory = DiffContentFactory.getInstance();
            DiffContent source = null;
            DiffContent updated = null;

            if (selectedPath != null) {
                Object lastComponent = selectedPath.getLastPathComponent();

                if (lastComponent.equals(lastElementPair)) {
                    return;
                }

                if (lastElementPair != null && lastReplacedElement != null && lastElementThatReplace != null) {
                    PsiElement lastElementThatReplaceCopy = factory.createStatementFromText(lastElementThatReplace.getText(), null);

                    lastReplacedElement = lastElementThatReplace.replace(lastReplacedElement);
                    lastElementPair.setInitialPsiElement(lastReplacedElement);
                    lastElementPair.setUpdatedPsiElement(lastElementThatReplaceCopy);

                    lastElementPair = null;
                    lastReplacedElement = null;
                    lastElementThatReplace = null;
                }

                if (lastComponent instanceof PsiMethodPair) {
                    PsiMethodPair methodPair = (PsiMethodPair) lastComponent;

                    source = contentFactory.create(project, methodPair.getInitialPsiMethod().getText(), JavaFileType.INSTANCE);
                    updated = contentFactory.create(project, methodPair.getUpdatedPsiMethod().getText(), JavaFileType.INSTANCE);
                } else if (lastComponent instanceof PsiElementPair) {
                    PsiElementPair elementPair = (PsiElementPair) lastComponent;
                    lastElementPair = elementPair;

                    PsiElement sourceElement = elementPair.getInitialPsiElement();
                    PsiElement extractedElement = elementPair.getUpdatedPsiElement();
                    PsiMethod initialMethod = elementPair.getInitialMethod();

                    lastReplacedElement = factory.createStatementFromText(sourceElement.getText(), null);

                    source = contentFactory.create(project, initialMethod.copy().getText(), JavaFileType.INSTANCE);

                    extractedElement = sourceElement.replace(extractedElement);
                    lastElementThatReplace = extractedElement;

                    updated = contentFactory.create(project, initialMethod.getText(), JavaFileType.INSTANCE);
                }
            }

            if (source != null && updated != null) {
                myChain.setContent1(source);
                myChain.setContent2(updated);
                getProcessor().updateRequest();
            }
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
                    return previewProcessor.getMethodElementsComparingMap().get(initialMethod).get(index);
                } else if (parent instanceof PsiElementPair) {
                    return null;
                }

                return previewProcessor.getMethodComparingList().get(index);
            }

            @Override
            public int getChildCount(Object parent) {
                if (parent instanceof PsiMethodPair) {
                    PsiMethod initialMethod = ((PsiMethodPair) parent).getInitialPsiMethod();
                    return previewProcessor.getMethodElementsComparingMap().get(initialMethod).size();
                } else if (parent instanceof PsiElementPair) {
                    return 0;
                }

                return previewProcessor.getMethodComparingList().size();
            }
        }
    }
}
