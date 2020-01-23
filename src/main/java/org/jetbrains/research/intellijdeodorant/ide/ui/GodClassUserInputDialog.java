package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.actions.impl.MutableDiffRequestChain;
import com.intellij.diff.contents.DiffContent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.refactoring.RefactorJBundle;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.ui.components.JBLabelDecorator;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassRefactoringType.AbstractExtractClassRefactoring;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GodClassUserInputDialog extends RefactoringDialog {
    private static final String TITLE = IntelliJDeodorantBundle.message("god.class.dialog.title");
    private static final String RESTORE_DEFAULT = IntelliJDeodorantBundle.message("god.class.dialog.restore.default");
    private static final String CLASS_NAME_NOT_VALID = IntelliJDeodorantBundle.message("god.class.dialog.class.name.not.valid");
    private static final String CLASS_NAME_ALREADY_EXISTS_IN_JAVA_LANG = IntelliJDeodorantBundle.message("god.class.dialog.class.name.already.exists.javalang");
    private static final String CLASS_NAME_ALREADY_EXISTS_KEY = "god.class.dialog.class.name.already.exists";

    private static final int MAIN_PANEL_VERTICAL_GAP = 5;

    private AbstractExtractClassRefactoring abstractRefactoring;
    private ExtractClassRefactoring refactoring;
    @Nullable
    private PsiPackage parentPackage;
    private List<String> javaLangClassNames;
    private JPanel mainPanel;
    private JTextField extractedClassNameField = new JTextField();
    private JButton restoreButton = new JButton();
    private AbstractRefactoringPanel godClassPanel;

    public GodClassUserInputDialog(AbstractExtractClassRefactoring abstractRefactoring, AbstractRefactoringPanel godClassPanel) {
        super(abstractRefactoring.getRefactoring().getSourceFile().getProject(), true);

        this.abstractRefactoring = abstractRefactoring;
        this.refactoring = abstractRefactoring.getRefactoring();
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
        this.godClassPanel = godClassPanel;

        String packageName = PsiUtil.getPackageName(refactoring.getSourceClass());
        parentPackage = JavaPsiFacade.getInstance(refactoring.getProject()).findPackage(packageName);

        initialiseControls();
        setTitle(TITLE);
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
        return null;
    }

    private void initialiseControls() {
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

        restoreButton.setText(RESTORE_DEFAULT);

        restoreButton.addActionListener(e -> {
            extractedClassNameField.setText(refactoring.getDefaultExtractedTypeName());
        });

        handleInputChanged(extractedClassNameField);
    }

    private void placeControlsOnPanel() {
        FormBuilder builder = FormBuilder.createFormBuilder()
                .addComponent(
                        JBLabelDecorator.createJBLabelDecorator(RefactorJBundle.message("extract.class.from.label", PsiTreeUtil.findChildOfType(refactoring.getSourceFile(), PsiClass.class)))
                                .setBold(true))
                .addLabeledComponent(RefactorJBundle.message("name.for.new.class.label"), extractedClassNameField, UIUtil.LARGE_VGAP);

        JComponent emptyComponent = new JComponent() {
            @Override
            public void setInheritsPopupMenu(boolean value) {
                super.setInheritsPopupMenu(value);
            }
        };

        builder.addLabeledComponent(restoreButton, emptyComponent);

        mainPanel = builder.addVerticalGap(MAIN_PANEL_VERTICAL_GAP).getPanel();
    }

    private void setMessage(String message) {
        setErrorText(message);
    }

    private void mayApplyRefactoring(boolean value) {
        getRefactorAction().setEnabled(value);
        getPreviewAction().setEnabled(value);
    }

    private void handleInputChanged(JTextField textField) {
        String className = textField.getText();
        String classNamePattern = "[a-zA-Z_][a-zA-Z0-9_]*";
        if (!Pattern.matches(classNamePattern, className)) {
            mayApplyRefactoring(false);
            setMessage(CLASS_NAME_NOT_VALID);
            return;
        } else if (parentPackage != null && parentPackage.containsClassNamed(className)) {
            mayApplyRefactoring(false);
            setMessage(IntelliJDeodorantBundle.message(CLASS_NAME_ALREADY_EXISTS_KEY, parentPackage.getName()));
            return;
        } else if (javaLangClassNames.contains(className)) {
            mayApplyRefactoring(false);
            setMessage(CLASS_NAME_ALREADY_EXISTS_IN_JAVA_LANG);
            return;
        } else {
            refactoring.setExtractedTypeName(className);
        }

        mayApplyRefactoring(true);
        setMessage("");
    }

    @Override
    protected void doAction() {
        if (isPreviewUsages()) {
            godClassPanel.setPreviewUsage(true);

            DiffContentFactory contentFactory = DiffContentFactory.getInstance();
            DiffContent c1 = contentFactory.create("");
            DiffContent c2 = contentFactory.create("");

            MutableDiffRequestChain chain = new MutableDiffRequestChain(c1, c2);

            refactoring.setPreviewUsage();

            WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                refactoring.apply();
            });

            GodClassPreviewResultDialog previewResultDialog = new GodClassPreviewResultDialog(getProject(), chain, DiffDialogHints.DEFAULT, refactoring.getPreviewProcessor());
            previewResultDialog.show();

            refactoring = abstractRefactoring.renewRefactoring();

            setPreviewResults(false);
            godClassPanel.setPreviewUsage(false);
        } else {
            closeOKAction();
            refactoring.setExtractedTypeName(extractedClassNameField.getText());
            WriteCommandAction.runWriteCommandAction(refactoring.getProject(), () -> refactoring.apply());
        }
    }

    @Override
    protected boolean hasHelpAction() {
        return false;
    }
}
