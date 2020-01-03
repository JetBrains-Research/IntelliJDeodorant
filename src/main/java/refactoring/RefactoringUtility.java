package refactoring;

import com.intellij.psi.*;
import org.jetbrains.research.intellijdeodorant.core.ast.ASTReader;
import org.jetbrains.research.intellijdeodorant.core.ast.ClassObject;
import org.jetbrains.research.intellijdeodorant.core.ast.SystemObject;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg.AbstractVariable;

public class RefactoringUtility {
    public static PsiClassType generateTypeFromTypeBinding(PsiClass psiClass, PsiElementFactory factory) {
        return factory.createType(psiClass);
    }

    public static PsiField findFieldDeclaration(AbstractVariable variable, PsiClass typeDeclaration) {
        for (PsiField fieldDeclaration : typeDeclaration.getFields()) {
            if (variable.getOrigin().equals(fieldDeclaration)) {
                return fieldDeclaration;
            }
        }
        //fragment was not found in typeDeclaration
        PsiClass superclassType = typeDeclaration.getSuperClass();
        if (superclassType != null) {
            String superclassQualifiedName = superclassType.getQualifiedName();
            SystemObject system = ASTReader.getSystemObject();
            ClassObject superclassObject = system.getClassObject(superclassQualifiedName);
            if (superclassObject != null) {
                return findFieldDeclaration(variable, superclassType);
            }
        }
        return null;
    }

    public static PsiClass findDeclaringTypeDeclaration(PsiField variableBinding, PsiClass typeDeclaration) {
        if (typeDeclaration.equals(variableBinding.getContainingClass())) {
            return typeDeclaration;
        }
        //fragment was not found in typeDeclaration
        PsiClass superclassType = typeDeclaration.getSuperClass();
        if (superclassType != null) {
            String superclassQualifiedName = superclassType.getQualifiedName();
            SystemObject system = ASTReader.getSystemObject();
            ClassObject superclassObject = system.getClassObject(superclassQualifiedName);
            if (superclassObject != null) {
                return findDeclaringTypeDeclaration(variableBinding, superclassType);
            }
        }
        return null;
    }

    public static PsiClass findDeclaringTypeDeclaration(PsiMethod methodBinding, PsiClass typeDeclaration) {
        if (typeDeclaration.equals(methodBinding.getContainingClass())) {
            return typeDeclaration;
        }
        //method was not found in typeDeclaration
        PsiClass superclassType = typeDeclaration.getSuperClass();
        if (superclassType != null) {
            String superclassQualifiedName = superclassType.getQualifiedName();
            SystemObject system = ASTReader.getSystemObject();
            ClassObject superclassObject = system.getClassObject(superclassQualifiedName);
            if (superclassObject != null) {
                findDeclaringTypeDeclaration(methodBinding, superclassType);
            }
        }
        return null;
    }

    public static boolean needsQualifier(PsiReferenceExpression simpleName) {
        return (!(simpleName instanceof PsiQualifiedExpression) || isArrayLengthQualifiedName(simpleName)) &&
                !isEnumConstantInSwitchCaseExpression(simpleName);
    }

    private static boolean isArrayLengthQualifiedName(PsiReferenceExpression simpleName) {
        return simpleName instanceof PsiQualifiedExpression && ((PsiQualifiedExpression) simpleName).getQualifier().getText().equals("length");
    }

    private static boolean isEnumConstantInSwitchCaseExpression(PsiReferenceExpression simpleName) {
        PsiElement binding = simpleName.resolve();
        if (binding instanceof PsiVariable) {
            PsiVariable variableBinding = (PsiVariable) binding;
            if (variableBinding instanceof PsiField) {
                return simpleName instanceof PsiSwitchLabeledRuleStatement && ((PsiField) variableBinding).getContainingClass().isEnum();
            }
        }
        return false;
    }
}
