package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiVariable;

import java.util.Set;

class PDGSynchronizedNode extends PDGBlockNode {
    PDGSynchronizedNode(CFGSynchronizedNode cfgSynchronizedNode, Set<PsiVariable> variableDeclarationsInMethod,
                        Set<PsiField> fieldsAccessedInMethod) {
        super(cfgSynchronizedNode, variableDeclarationsInMethod, fieldsAccessedInMethod);
        this.controlParent = cfgSynchronizedNode.getControlParent();
        determineDefinedAndUsedVariables();
    }
}
