package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.actions.impl.MutableDiffRequestChain;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.impl.CacheDiffRequestChainProcessor;
import com.intellij.diff.impl.DiffRequestProcessor;
import com.intellij.diff.impl.DiffWindow;
import com.intellij.diff.util.DiffUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassPreviewProcessor;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassPreviewProcessor.PsiClassWrapper;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassPreviewProcessor.PsiElementChange;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassPreviewProcessor.PsiElementComparingPair;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassPreviewProcessor.PsiMethodComparingPair;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.ui.listeners.ElementSelectionListener;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassPreviewProcessor.PsiElementChange.CHANGE_TYPE.ADD_AFTER;
import static org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassPreviewProcessor.PsiElementChange.CHANGE_TYPE.ADD_BEFORE;

public class GodClassPreviewResultDialog extends DiffWindow {
    private MutableDiffRequestChain myChain;
    private ExtractClassPreviewProcessor previewProcessor;
    private Project project;
    DiffContentFactory diffContentFactory;
    private JavaCodeStyleManager javaCodeStyleManager;
    private CodeStyleManager codeStyleManager;

    public GodClassPreviewResultDialog(@NotNull Project project, @NotNull MutableDiffRequestChain requestChain, @NotNull DiffDialogHints hints, ExtractClassPreviewProcessor previewProcessor) {
        super(project, requestChain, hints);
        this.myChain = requestChain;
        this.project = project;
        this.diffContentFactory = DiffContentFactory.getInstance();
        this.previewProcessor = previewProcessor;

        javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
        codeStyleManager = CodeStyleManager.getInstance(project);
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

            String source = "";
            String updated = "";

            if (selectedPath != null) {
                Object lastComponent = selectedPath.getLastPathComponent();

                if (lastComponent == previewProcessor.getExtractedClass()) {
                    source = "";
                    updated = getTextAndReformat(previewProcessor.getExtractedClass().getPsiClass());
                } else if (lastComponent instanceof PsiClassWrapper) {
                    source = getTextAndReformat(previewProcessor.getInitialSourceClass().getPsiClass());
                    updated = getTextAndReformat(previewProcessor.getUpdatedSourceClass().getPsiClass());
                } else if (lastComponent instanceof PsiElementChange) {
                    PsiElementChange elementChange = (PsiElementChange) lastComponent;

                    PsiElement parent = null;
                    switch (elementChange.getChange_type()) {
                        case ADD_AFTER: case ADD_BEFORE: {
                            parent = elementChange.getAnchor().getParent().getParent(); //PsiElement -> PsiCodeBlock -> PsiBodyOwner
                            break;
                        }
                        case REPLACE: {
                            parent = elementChange.getPsiElement().getParent().getParent(); //PsiElement -> PsiCodeBlock -> PsiBodyOwner
                            break;
                        }
                        case REMOVE: {
                            parent = elementChange.getPsiElement().getParent(); //for instance, PsiField -> PsiClass
                            break;
                        }
                    }

                    PsiElement parentCopy = parent.copy();
                    Map<PsiElement, PsiElement> initialToCopy = new HashMap<>();
                    ExtractClassPreviewProcessor.mapElementsToCopy(parent, parentCopy, initialToCopy);

                    switch (elementChange.getChange_type()) {
                        case ADD_BEFORE: case ADD_AFTER: {
                            source = getTextAndReformat(parent);

                            PsiElement elementToAdd = initialToCopy.get(elementChange.getPsiElement());
                            if (elementToAdd == null) {
                                elementToAdd = elementChange.getPsiElement(); //Element to add can be newly created
                            }

                            PsiElement anchor = initialToCopy.get(elementChange.getAnchor());

                            if (elementChange.getChange_type() == ADD_BEFORE) {
                                anchor.getParent().addBefore(elementToAdd, anchor);
                            } else if (elementChange.getChange_type() == ADD_AFTER) {
                                anchor.getParent().addAfter(elementToAdd, anchor);
                            }
                            updated = getTextAndReformat(parentCopy);
                            break;
                        }
                        case REPLACE: {
                            source = getTextAndReformat(parent);
                            PsiElement elementToReplace = initialToCopy.get(elementChange.getPsiElement());
                            PsiElement replacingElement = initialToCopy.get(elementChange.getAnchor());
                            elementToReplace.replace(replacingElement);
                            updated = getTextAndReformat(parentCopy);
                            break;
                        }
                        case REMOVE: {
                            source = getTextAndReformat(parent);

                            PsiElement elementToRemove = initialToCopy.get(elementChange.getPsiElement());
                            elementToRemove.delete();
                            updated = getTextAndReformat(parentCopy);
                            break;
                        }
                    }
                } else if (lastComponent instanceof PsiMethodComparingPair) {
                    PsiMethodComparingPair methodPair = (PsiMethodComparingPair) lastComponent;

                    source = getTextAndReformat(methodPair.getInitialPsiMethod().copy());
                    updated = getTextAndReformat(methodPair.getUpdatedPsiMethod().copy());
                } else if (lastComponent instanceof PsiElementComparingPair) {
                    PsiElementComparingPair elementPair = (PsiElementComparingPair) lastComponent;

                    PsiElement sourceElement = elementPair.getInitialPsiElement();
                    PsiElement extractedElement = elementPair.getUpdatedPsiElement();

                    PsiMethod initialMethod = elementPair.getInitialMethod();
                    source = getTextAndReformat(initialMethod.copy());

                    PsiElement copyMethod = initialMethod.copy();
                    Map<PsiElement, PsiElement> initialToCopy = new HashMap<>();
                    ExtractClassPreviewProcessor.mapElementsToCopy(initialMethod, copyMethod, initialToCopy);

                    sourceElement = initialToCopy.get(sourceElement);
                    sourceElement.replace(extractedElement);

                    updated = getTextAndReformat(copyMethod);
                }
            }

            if (source != null && updated != null) {
                myChain.setContent1(diffContentFactory.create(project, source, JavaFileType.INSTANCE));
                myChain.setContent2(diffContentFactory.create(project, updated, JavaFileType.INSTANCE));
                getProcessor().updateRequest();
            }
        }

        public String getTextAndReformat(PsiElement element) {
            if (element == null) {
                return "";
            }

            element = codeStyleManager.reformat(element);
            element = javaCodeStyleManager.shortenClassReferences(element);
            return element.getText();
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
                if (node instanceof PsiMethodComparingPair) {
                    return ((PsiMethodComparingPair) node).getInitialPsiMethod().getName();
                } else if (node instanceof PsiElementComparingPair) {
                    return ((PsiElementComparingPair) node).getInitialPsiElement().getText();
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
                if (node instanceof PsiMethodComparingPair && getChildCount(node) == 0) {
                    return true;
                }

                return node instanceof PsiElementComparingPair ||
                        node instanceof PsiElementChange ||
                        node == previewProcessor.getExtractedClass();
            }

            @Override
            public Object getChild(Object parent, int index) {
                if (parent instanceof PsiMethodComparingPair) {
                    PsiMethod initialMethod = ((PsiMethodComparingPair) parent).getInitialPsiMethod();
                    List<PsiElementComparingPair> children =  previewProcessor.getMethodElementsComparingMap().get(initialMethod);
                    if (children != null) {
                        return children.get(index);
                    }
                } else if (parent instanceof PsiClassWrapper) {
                    if (index < previewProcessor.getMethodComparingList().size()) {
                        return previewProcessor.getMethodComparingList().get(index);
                    } else {
                        index -= previewProcessor.getMethodComparingList().size();
                        return previewProcessor.getPsiElementChanges().get(index);
                    }
                }

                if (index == 0) {
                    return previewProcessor.getInitialSourceClass();
                } else {
                    return previewProcessor.getExtractedClass();
                }
            }

            @Override
            public int getChildCount(Object parent) {
                if (parent instanceof PsiMethodComparingPair) {
                    PsiMethod initialMethod = ((PsiMethodComparingPair) parent).getInitialPsiMethod();
                    List<PsiElementComparingPair> children = previewProcessor.getMethodElementsComparingMap().get(initialMethod);
                    if (children != null) {
                        return children.size();
                    } else {
                        return 0;
                    }
                } else if (parent instanceof PsiElementComparingPair) {
                    return 0;
                } else if (parent == previewProcessor.getExtractedClass()) {
                    return 0;
                } else if (parent instanceof PsiClassWrapper) {
                    return previewProcessor.getMethodComparingList().size() + previewProcessor.getPsiElementChanges().size();
                } else if (parent instanceof PsiElementChange) {
                    return 0;
                }

                return 2; //source and extracted class
            }
        }
    }
}
