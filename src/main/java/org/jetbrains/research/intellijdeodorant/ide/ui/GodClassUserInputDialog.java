package org.jetbrains.research.intellijdeodorant.ide.ui;

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

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class GodClassUserInputDialog extends RefactoringDialog {
    private static final String TITLE = IntelliJDeodorantBundle.message("god.class.dialog.title");
    private static final String RESTORE_DEFAULT = IntelliJDeodorantBundle.message("god.class.dialog.restore.default");
    private static final String CLASS_NAME_NOT_VALID = IntelliJDeodorantBundle.message("god.class.dialog.class.name.not.valid");
    private static final String CLASS_NAME_ALREADY_EXISTS_IN_JAVA_LANG = IntelliJDeodorantBundle.message("god.class.dialog.class.name.already.exists.javalang");
    private static final String CLASS_NAME_ALREADY_EXISTS_KEY = "god.class.dialog.class.name.already.exists";

    private static final int MAIN_PANEL_VERTICAL_GAP = 5;

    private final ExtractClassRefactoring refactoring;
    @Nullable
    private final PsiPackage parentPackage;
    private final List<String> javaLangClassNames = new ArrayList<>(Arrays.asList(
            "Boolean", "Byte", "Character", "Class", "Double", "Enum", "Error", "Exception",
            "Float", "Integer", "Long", "Math", "Number", "Object", "Package", "Process",
            "Runtime", "Short", "String", "StringBuffer", "StringBuilder", "System", "Thread", "Void"));
    ;
    private JPanel mainPanel;
    private final JTextField extractedClassNameField = new JTextField();
    private final JButton restoreButton = new JButton();

    public GodClassUserInputDialog(ExtractClassRefactoring refactoring) {
        super(refactoring.getSourceFile().getProject(), true);

        this.refactoring = refactoring;
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

        restoreButton.addActionListener(e -> extractedClassNameField.setText(refactoring.getDefaultExtractedTypeName()));

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
        closeOKAction();
        refactoring.setExtractedTypeName(extractedClassNameField.getText());
        WriteCommandAction.runWriteCommandAction(refactoring.getProject(), refactoring::apply);
    }

    @Override
    protected boolean hasHelpAction() {
        return false;
    }

    @Override
    protected boolean hasPreviewButton() {
        return false;
    }
}
