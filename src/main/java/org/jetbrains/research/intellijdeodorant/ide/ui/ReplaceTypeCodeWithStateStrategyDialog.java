package org.jetbrains.research.intellijdeodorant.ide.ui;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.util.PsiUtil;
import com.intellij.refactoring.ui.RefactoringDialog;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.intellijdeodorant.IntelliJDeodorantBundle;
import org.jetbrains.research.intellijdeodorant.ide.refactoring.typeStateChecking.ReplaceTypeCodeWithStateStrategy;

import javax.swing.*;
import java.util.*;
import java.util.regex.Pattern;

import com.intellij.psi.*;
import com.intellij.util.ui.FormBuilder;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ReplaceTypeCodeWithStateStrategyDialog extends RefactoringDialog {
    private final ReplaceTypeCodeWithStateStrategy refactoring;
    private final Map<JTextField, PsiField> textMap = new HashMap<>();
    private final Map<JTextField, String> defaultNamingMap = new HashMap<>();

    @Nullable
    private final PsiPackage parentPackage;
    private final List<String> javaLangClassNames = new ArrayList<>(Arrays.asList(
            "Boolean", "Byte", "Character", "Class", "Double", "Enum", "Error", "Exception",
            "Float", "Integer", "Long", "Math", "Number", "Object", "Package", "Process",
            "Runtime", "Short", "String", "StringBuffer", "StringBuilder", "System", "Thread", "Void"));
    private final Runnable applyRefactoringCallback;
    private JPanel mainPanel;
    private final JButton restoreButton = new JButton();

    public ReplaceTypeCodeWithStateStrategyDialog(ReplaceTypeCodeWithStateStrategy refactoring,
                                                  Runnable applyRefactoringCallback) {
        super(refactoring.getProject(), true);

        this.refactoring = refactoring;
        String packageName = PsiUtil.getPackageName(refactoring.getSourceTypeDeclaration());
        parentPackage = JavaPsiFacade.getInstance(refactoring.getProject()).findPackage(packageName);
        this.applyRefactoringCallback = applyRefactoringCallback;
        init();
        setTitle(IntelliJDeodorantBundle.message("replace.type.code.with.state.strategy.name"));
        setListeners();
    }

    @Nullable
    @Override
    protected JComponent createNorthPanel() {
        initializePanel();
        return mainPanel;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return null;
    }

    private void setListeners() {
        for (JTextField field : textMap.keySet()) {
            field.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    handleInputChanged();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    handleInputChanged();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    handleInputChanged();
                }
            });
        }

        restoreButton.addActionListener(e -> {
            for (JTextField field : defaultNamingMap.keySet()) {
                field.setText(defaultNamingMap.get(field));
            }
        });

        handleInputChanged();
    }

    private void initializePanel() {
        FormBuilder builder = FormBuilder.createFormBuilder();

        builder.addLabeledComponent(IntelliJDeodorantBundle.message("replace.type.code.with.state.strategy.state.variable"),
                new JLabel(IntelliJDeodorantBundle.message("replace.type.code.with.state.strategy.abstract.state.strategy.type.name")));
        JTextField stateVariableTextField = new JTextField(refactoring.getAbstractClassName());
        textMap.put(stateVariableTextField, null);
        defaultNamingMap.put(stateVariableTextField, refactoring.getAbstractClassName());
        builder.addLabeledComponent(refactoring.getTypeVariableSimpleName(), stateVariableTextField);

        builder.addLabeledComponent(IntelliJDeodorantBundle.message("replace.type.code.with.state.strategy.named.constants"),
                new JLabel(IntelliJDeodorantBundle.message("replace.type.code.with.state.strategy.concrete.state.strategy.type.name")), 10);
        for (Map.Entry<PsiField, String> entry : refactoring.getStaticFieldMapEntrySet()) {
            JTextField textField = new JTextField(entry.getValue());
            textMap.put(textField, entry.getKey());
            defaultNamingMap.put(textField, entry.getValue());
            builder.addLabeledComponent(entry.getValue(), textField);
        }

        for (Map.Entry<PsiField, String> entry : refactoring.getAdditionalStaticFieldMapEntrySet()) {
            JTextField textField = new JTextField(entry.getValue());
            textMap.put(textField, entry.getKey());
            defaultNamingMap.put(textField, entry.getValue());
            builder.addLabeledComponent(entry.getValue(), textField);
        }

        JComponent emptyComponent = new JComponent() {
            @Override
            public void setInheritsPopupMenu(boolean value) {
                super.setInheritsPopupMenu(value);
            }
        };

        restoreButton.setText(IntelliJDeodorantBundle.message("replace.type.code.with.state.strategy.restore.title"));
        builder.addLabeledComponent(restoreButton, emptyComponent, 10);

        mainPanel = builder.addVerticalGap(5).getPanel();
    }

    private void mayApplyRefactoring(boolean value) {
        getRefactorAction().setEnabled(value);
    }

    private void handleInputChanged() {
        String classNamePattern = "[a-zA-Z_][a-zA-Z0-9_]*";
        Set<String> encounteredNames = new HashSet<>();
        List<ValidationInfo> validationInfoList = new ArrayList<>();
        for (JTextField textField : textMap.keySet()) {
            String text = textField.getText();
            if (!Pattern.matches(classNamePattern, textField.getText())) {
                validationInfoList.add(new ValidationInfo(IntelliJDeodorantBundle.message("replace.type.code.with.state.strategy.dialog.error.invalid"), textField));
            } else if (parentPackage != null && parentPackage.containsClassNamed(text)) {
                validationInfoList.add(new ValidationInfo(IntelliJDeodorantBundle.message("replace.type.code.with.state.strategy.dialog.error.exists.package"), textField));
            } else if (javaLangClassNames.contains(text)) {
                validationInfoList.add(new ValidationInfo(IntelliJDeodorantBundle.message("replace.type.code.with.state.strategy.dialog.error.exists.javalang"), textField));
            } else if (encounteredNames.contains(text)) {
                validationInfoList.add(new ValidationInfo(IntelliJDeodorantBundle.message("replace.type.code.with.state.strategy.dialog.error.chosen"), textField));
            } else {
                refactoring.setTypeNameForNamedConstant(textMap.get(textField), textField.getText());
            }
            encounteredNames.add(text);
        }
        setErrorInfoAll(validationInfoList);
        mayApplyRefactoring(validationInfoList.isEmpty());
    }

    @Override
    protected void doAction() {
        close(OK_EXIT_CODE);
        applyRefactoringCallback.run();
    }

    protected boolean hasHelpAction() {
        return false;
    }

    @Override
    protected boolean hasPreviewButton() {
        return false;
    }
}
