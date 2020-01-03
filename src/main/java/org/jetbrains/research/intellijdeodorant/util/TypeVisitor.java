package org.jetbrains.research.intellijdeodorant.ast.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TypeVisitor extends PsiElementVisitor {
	private Set<PsiType> typeBindings = new LinkedHashSet<>();
	private List<PsiTypeElement> types = new ArrayList<>();

	@Override
	public void visitElement(PsiElement node) {
	    if (node instanceof PsiTypeElement) {
	        PsiTypeElement psiTypeElement = (PsiTypeElement) node;
            PsiType typeBinding = psiTypeElement.getType();
            if (typeBinding != null)
                typeBindings.add(typeBinding);
            types.add(psiTypeElement);
        }

        super.visitElement(node);
	}

	/*
	public boolean visit(SimpleName node) {
		IBinding binding = node.resolveBinding();
		if(binding != null && binding.getKind() == IBinding.TYPE) {
			ITypeBinding typeBinding = (ITypeBinding)binding;
			typeBindings.add(typeBinding);
		}
		return super.visit(node);
	}

	 */

	public Set<PsiType> getTypeBindings() {
		return typeBindings;
	}

	public List<PsiTypeElement> getTypes() {
		return types;
	}
}