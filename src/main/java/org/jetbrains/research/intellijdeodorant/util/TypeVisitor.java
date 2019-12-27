package core.ast.util;

import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.List;

public class TypeVisitor extends PsiElementVisitor {
	private List<PsiType> types = new ArrayList<>();

	@Override
	public void visitElement(PsiElement element) {
		if (element instanceof PsiTypeElement) {
			types.add(((PsiTypeElement) element).getType());
		}
	}

	public List<PsiType> getTypes() {
		return types;
	}
}