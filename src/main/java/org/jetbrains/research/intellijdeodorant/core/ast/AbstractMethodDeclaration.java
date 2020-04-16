package org.jetbrains.research.intellijdeodorant.core.ast;

import java.util.List;
import java.util.ListIterator;

import com.intellij.psi.PsiMethod;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.MethodBodyObject;

public interface AbstractMethodDeclaration {

    String getName();

    PsiMethod getMethodDeclaration();

    MethodBodyObject getMethodBody();

    ListIterator<ParameterObject> getParameterListIterator();

    List<FieldInstructionObject> getFieldInstructions();

    List<LocalVariableDeclarationObject> getLocalVariableDeclarations();

}
