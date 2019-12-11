package ui;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiUtil;
import com.intellij.refactoring.ui.RefactoringDialog;
import org.jetbrains.annotations.Nullable;
import refactoring.ReplaceTypeCodeWithStateStrategy;

import javax.swing.*;
import java.util.*;
import java.util.regex.Pattern;

import com.intellij.psi.*;
import com.intellij.util.ui.FormBuilder;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReplaceTypeCodeWithStateStrategyDialog extends RefactoringDialog {

	private ReplaceTypeCodeWithStateStrategy refactoring;
	private Map<JTextField, PsiField> textMap = new HashMap<>();
	private Map<JTextField, String> defaultNamingMap = new HashMap<>();

	@Nullable
	private PsiPackage parentPackage;
	private List<String> javaLangClassNames;
	private Runnable applyRefactoringCallback;
	private JPanel mainPanel;
	private JButton restoreButton = new JButton();

	public ReplaceTypeCodeWithStateStrategyDialog(ReplaceTypeCodeWithStateStrategy refactoring,
												  Runnable applyRefactoringCallback) {
		super(refactoring.getProject(), true);

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

		String packageName = PsiUtil.getPackageName(refactoring.getSourceTypeDeclaration());
		parentPackage = JavaPsiFacade.getInstance(refactoring.getProject()).findPackage(packageName);
		this.applyRefactoringCallback = applyRefactoringCallback;
		init();
		setTitle("Replace Type Code with State/Strategy");
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
		for(JTextField field : textMap.keySet()) {
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
			for(JTextField field : defaultNamingMap.keySet()) {
				field.setText(defaultNamingMap.get(field));
			}
		});

		handleInputChanged();
	}

	private void initializePanel() {
		FormBuilder builder = FormBuilder.createFormBuilder();

		builder.addLabeledComponent("State Variable:", new JLabel("Abstract State/Strategy Type Name:"));
		JTextField stateVariableTextField = new JTextField(refactoring.getAbstractClassName());
		textMap.put(stateVariableTextField, null);
		defaultNamingMap.put(stateVariableTextField, refactoring.getAbstractClassName());
		builder.addLabeledComponent(refactoring.getTypeVariableSimpleName(), stateVariableTextField);

		builder.addLabeledComponent("Named Constants:", new JLabel("Concrete State/Strategy Type Names:"), 10);
		for(Map.Entry<PsiField, String> entry : refactoring.getStaticFieldMapEntrySet()) {
			JTextField textField = new JTextField(entry.getValue());
			textMap.put(textField, entry.getKey());
			defaultNamingMap.put(textField, entry.getValue());
			builder.addLabeledComponent(entry.getValue(), textField);
		}

		for(Map.Entry<PsiField, String> entry : refactoring.getAdditionalStaticFieldMapEntrySet()) {
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

		restoreButton.setText("Restore Defaults");
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
				String message = "Type name is not valid";
				validationInfoList.add(new ValidationInfo(message, textField));
			} else if (parentPackage != null && parentPackage.containsClassNamed(text)) {
				String message = "Type with this name already exists in the package";
				validationInfoList.add(new ValidationInfo(message, textField));
			} else if (javaLangClassNames.contains(text)) {
				String message = "Type with this name already exists in package java.lang";
				validationInfoList.add(new ValidationInfo(message, textField));
			} else if (encounteredNames.contains(text)) {
				String message = "This name is already chosen";
				validationInfoList.add(new ValidationInfo(message, textField));
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
