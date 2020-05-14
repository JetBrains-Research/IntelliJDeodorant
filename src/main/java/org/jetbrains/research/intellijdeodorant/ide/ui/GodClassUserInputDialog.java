package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.actions.impl.MutableDiffRequestChain;
import com.intellij.diff.contents.DiffContent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiUtil;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.ide.fus.collectors.IntelliJDeodorantCounterCollector;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassRefactoring;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.extractClass.ExtractClassRefactoringType.AbstractExtractClassRefactoring;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class GodClassUserInputDialog extends RefactoringDialog {
    private static final int MAIN_PANEL_VERTICAL_GAP = 5;
    private ExtractClassRefactoring refactoring;
    private AbstractExtractClassRefactoring abstractRefactoring;
    @Nullable
    private final PsiPackage parentPackage;
    private final List<String> javaLangClassNames = new ArrayList<>(Arrays.asList(
            "Boolean", "Byte", "Character", "Class", "Double", "Enum", "Error", "Exception",
            "Float", "Integer", "Long", "Math", "Number", "Object", "Package", "Process",
            "Runtime", "Short", "String", "StringBuffer", "StringBuilder", "System", "Thread", "Void"));
    private JPanel mainPanel;
    private final JTextField extractedClassNameField = new JTextField();
    private AbstractRefactoringPanel godClassPanel;

    public GodClassUserInputDialog(AbstractExtractClassRefactoring abstractRefactoring, AbstractRefactoringPanel godClassPanel) {
        super(abstractRefactoring.getRefactoring().getSourceFile().getProject(), true);
        this.abstractRefactoring = abstractRefactoring;
        this.refactoring = abstractRefactoring.getRefactoring();
        String packageName = PsiUtil.getPackageName(refactoring.getSourceClass());
        parentPackage = JavaPsiFacade.getInstance(refactoring.getProject()).findPackage(packageName);
        this.godClassPanel = godClassPanel;
        initialiseControls();
        setTitle(IntelliJDeodorantBundle.message("god.class.dialog.title"));
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

        handleInputChanged(extractedClassNameField);
    }

    private void placeControlsOnPanel() {
        FormBuilder builder = FormBuilder.createFormBuilder().addLabeledComponent(IntelliJDeodorantBundle.message("god.class.preview.new.class.name"), extractedClassNameField, UIUtil.LARGE_VGAP);
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
            setMessage(IntelliJDeodorantBundle.message("god.class.dialog.class.name.not.valid"));
            return;
        } else if (parentPackage != null && parentPackage.containsClassNamed(className)) {
            mayApplyRefactoring(false);
            setMessage(IntelliJDeodorantBundle.message("god.class.dialog.class.name.already.exists", parentPackage.getName()));
            return;
        } else if (javaLangClassNames.contains(className)) {
            mayApplyRefactoring(false);
            setMessage(IntelliJDeodorantBundle.message("god.class.dialog.class.name.already.exists.javalang"));
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
            WriteCommandAction.runWriteCommandAction(refactoring.getProject(), refactoring::apply);
            IntelliJDeodorantCounterCollector.getInstance().extractClassRefactoringApplied(refactoring.getProject(),
                    refactoring.getExtractedFieldFragmentsCount(),
                    refactoring.getExtractedMethodsCount(),
                    refactoring.getSourceClass().getFields().length,
                    refactoring.getSourceClass().getMethods().length);
        }
    }

    @Override
    protected boolean hasHelpAction() {
        return false;
    }
}
