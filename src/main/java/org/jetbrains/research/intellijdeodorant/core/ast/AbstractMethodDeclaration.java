package org.jetbrains.research.intellijdeodorant.core.ast;

import java.util.List;
import java.util.ListIterator;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiVariable;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.MethodBodyObject;

public interface AbstractMethodDeclaration {

    String getName();

    PsiMethod getMethodDeclaration();

    MethodBodyObject getMethodBody();

    ListIterator<PsiParameter> getParameterListIterator();

    List<PsiField> getFieldInstructions();

    List<PsiVariable> getLocalVariableDeclarations();

}
