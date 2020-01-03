package org.jetbrains.research.intellijdeodorant.ide.refactoring;

import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.chains.SimpleDiffRequestChain;
import com.intellij.diff.contents.FileDocumentContentImpl;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.refactoring.RefactorJBundle;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.ui.components.JBLabelDecorator;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.ExtractClassRefactoring;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GodClassUserInputDialog extends RefactoringDialog {
    private static final String TITLE = "Extracted Class Name";
    private ExtractClassRefactoring refactoring;
    @Nullable
    private PsiPackage parentPackage;
    private List<String> javaLangClassNames;
    private JPanel mainPanel;
    private Label extractedClassNameLabel = new Label();
    private JTextField extractedClassNameField = new JTextField(); //TODO default size
    private JButton restoreButton = new JButton();
    private JCheckBox delegateButton = new JCheckBox();

    public GodClassUserInputDialog(ExtractClassRefactoring refactoring) {
        super(refactoring.getSourceFile().getProject(), true);

        this.refactoring = refactoring;
        this.javaLangClassNames = new ArrayList<>();
        this.javaLangClassNames.add("Boolean");
        this.javaLangClassNames.add("Byte");
        this.javaLangClassNames.add("Character");
        this.javaLangClassNames.add("Class");
        this.javaLangClassNames.add("Double");
        this.javaLangClassNames.add("Enum");
        this.javaLangClassNames.add("Error");
        this.javaLangClassNames.add("Exception");
        this.javaLangClassNames.add("Float");
        this.javaLangClassNames.add("Integer");
        this.javaLangClassNames.add("Long");
        this.javaLangClassNames.add("Math");
        this.javaLangClassNames.add("Number");
        this.javaLangClassNames.add("Object");
        this.javaLangClassNames.add("Package");
        this.javaLangClassNames.add("Process");
        this.javaLangClassNames.add("Runtime");
        this.javaLangClassNames.add("Short");
        this.javaLangClassNames.add("String");
        this.javaLangClassNames.add("StringBuffer");
        this.javaLangClassNames.add("StringBuilder");
        this.javaLangClassNames.add("System");
        this.javaLangClassNames.add("Thread");
        this.javaLangClassNames.add("Void");

        PsiPackageStatement packageStatement = ((PsiJavaFile) refactoring.getSourceFile()).getPackageStatement();
        if (packageStatement != null) {
            PsiJavaCodeReferenceElement packageReference = packageStatement.getPackageReference();
            PsiElement target = packageReference.resolve();
            if (target instanceof PsiPackage) {
                parentPackage = (PsiPackage) target;
            }
        }

        initialiseControls();
        init();
    }

    @Nullable
    @Override
    protected JComponent createNorthPanel() {
        placeControlsOnPanel();
        return mainPanel;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return new JPanel();
    }

    private void initialiseControls() {
        extractedClassNameLabel.setText("Extracted Class Name");
        //extractedClassNameLabel.setFont(); //TODO set font

        //TODO extractedClassNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        extractedClassNameField.setText(refactoring.getExtractedTypeName());
        extractedClassNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleInputChanged(extractedClassNameField);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleInputChanged(extractedClassNameField);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleInputChanged(extractedClassNameField);
            }
        });

        restoreButton.setText("Restore Defaults");

        delegateButton.setText("Keep original public methods as delegates to the extracted methods");

        restoreButton.addActionListener(e -> {
            extractedClassNameField.setText(refactoring.getExtractedTypeName());
            delegateButton.setSelected(false);
            refactoring.setLeaveDelegateForPublicMethods(false);
        });

        delegateButton.addActionListener(e -> {
            JCheckBox source = (JCheckBox) e.getSource();
            if (source.isSelected())
                refactoring.setLeaveDelegateForPublicMethods(true);
            else
                refactoring.setLeaveDelegateForPublicMethods(false);
        });

        handleInputChanged(extractedClassNameField);

        getOKAction().addPropertyChangeListener(evt -> {
            refactoring.apply();
        });
    }

    private void placeControlsOnPanel() {
        final JPanel checkboxPanel = new JPanel(new BorderLayout());
        checkboxPanel.add(delegateButton, BorderLayout.WEST);
        FormBuilder builder = FormBuilder.createFormBuilder()
                .addComponent(
                        JBLabelDecorator.createJBLabelDecorator(RefactorJBundle.message("extract.class.from.label", refactoring.getSourceFile().getName())) //TODO class name here, not file
                                .setBold(true))
                .addLabeledComponent(RefactorJBundle.message("name.for.new.class.label"), extractedClassNameField, UIUtil.LARGE_VGAP)
                .addLabeledComponent(restoreButton, checkboxPanel);

        mainPanel = builder.addVerticalGap(5).getPanel();
    }

    private void setMessage(String message) {
        setErrorText(message);
    }

    private void setPageComplete(boolean value) {
        getOKAction().setEnabled(value);
        getPreviewAction().setEnabled(value);
    }

    private void handleInputChanged(JTextField textField) {
        String classNamePattern = "[a-zA-Z_][a-zA-Z0-9_]*";
        if (!Pattern.matches(classNamePattern, textField.getText())) {
            setPageComplete(false);
            String message = "Type name \"" + textField.getText() + "\" is not valid";
            setMessage(message);
            return;
        } else if (parentPackage != null) {
            PsiPackage lookuppackage = parentPackage;
            while (lookuppackage != null) {
                if (lookuppackage.containsClassNamed(textField.getText())) {
                    setPageComplete(false);
                    String message = "A Type named \"" + textField.getText() + "\" already exists in package " + parentPackage.getName();
                    setMessage(message);
                    return;
                }

                lookuppackage = lookuppackage.getParentPackage();
            }
        } else if (javaLangClassNames.contains(textField.getText())) {
            setPageComplete(false);
            String message = "Type \"" + textField.getText() + "\" already exists in package java.lang";
            setMessage(message);
            return;
        } else {
            refactoring.setExtractedTypeName(textField.getText());
        }

        setPageComplete(true);
        setMessage("");
    }

    @Override
    protected void doAction() {
        if (isPreviewUsages()) {
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
        }
    }

    protected boolean hasHelpAction() {
        return false;
    }
}
