package org.jetbrains.research.intellijdeodorant.core.ast;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import com.intellij.psi.PsiMethod;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.MethodBodyObject;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.AbstractVariable;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.PlainVariable;

public interface AbstractMethodDeclaration {

    String getName();

    Access getAccess();

    PsiMethod getMethodDeclaration();

    MethodBodyObject getMethodBody();

    String getClassName();

    ListIterator<CommentObject> getCommentListIterator();

    ListIterator<ParameterObject> getParameterListIterator();

    ParameterObject getParameter(int position);

    List<MethodInvocationObject> getMethodInvocations();

    List<SuperMethodInvocationObject> getSuperMethodInvocations();

    List<FieldInstructionObject> getFieldInstructions();

    List<SuperFieldInstructionObject> getSuperFieldInstructions();

    List<LocalVariableDeclarationObject> getLocalVariableDeclarations();

    List<LocalVariableInstructionObject> getLocalVariableInstructions();

    List<CreationObject> getCreations();

    List<LiteralObject> getLiterals();

    List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations();

    Set<String> getExceptionsInThrowStatements();

    Set<String> getExceptionsInJavaDocThrows();

    boolean containsMethodInvocation(MethodInvocationObject methodInvocation);

    boolean containsFieldInstruction(FieldInstructionObject fieldInstruction);

    boolean containsSuperMethodInvocation(SuperMethodInvocationObject superMethodInvocation);

    Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughFields();

    Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughFields();

    Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughParameters();

    Map<AbstractVariable, ArrayList<MethodInvocationObject>> getNonDistinctInvokedMethodsThroughParameters();

    Map<AbstractVariable, LinkedHashSet<MethodInvocationObject>> getInvokedMethodsThroughLocalVariables();

    Set<MethodInvocationObject> getInvokedMethodsThroughThisReference();

    List<MethodInvocationObject> getNonDistinctInvokedMethodsThroughThisReference();

    Set<MethodInvocationObject> getInvokedStaticMethods();

    Set<AbstractVariable> getDefinedFieldsThroughFields();

    Set<AbstractVariable> getUsedFieldsThroughFields();

    List<AbstractVariable> getNonDistinctDefinedFieldsThroughFields();

    List<AbstractVariable> getNonDistinctUsedFieldsThroughFields();

    Set<AbstractVariable> getDefinedFieldsThroughParameters();

    Set<AbstractVariable> getUsedFieldsThroughParameters();

    List<AbstractVariable> getNonDistinctDefinedFieldsThroughParameters();

    List<AbstractVariable> getNonDistinctUsedFieldsThroughParameters();

    Set<AbstractVariable> getDefinedFieldsThroughLocalVariables();

    Set<AbstractVariable> getUsedFieldsThroughLocalVariables();

    Set<PlainVariable> getDefinedFieldsThroughThisReference();

    List<PlainVariable> getNonDistinctDefinedFieldsThroughThisReference();

    Set<PlainVariable> getUsedFieldsThroughThisReference();

    List<PlainVariable> getNonDistinctUsedFieldsThroughThisReference();

    Set<PlainVariable> getDeclaredLocalVariables();

    Set<PlainVariable> getDefinedLocalVariables();

    Set<PlainVariable> getUsedLocalVariables();

    Map<PlainVariable, LinkedHashSet<MethodInvocationObject>> getParametersPassedAsArgumentsInMethodInvocations();

    Map<PlainVariable, LinkedHashSet<SuperMethodInvocationObject>> getParametersPassedAsArgumentsInSuperMethodInvocations();

    boolean containsSuperMethodInvocation();

    boolean containsSuperFieldAccess();

    List<TypeObject> getParameterTypeList();

    List<String> getParameterList();

    String getSignature();

    boolean isAbstract();
}
