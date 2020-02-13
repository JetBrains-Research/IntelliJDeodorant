package org.jetbrains.research.intellijdeodorant.ast;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.core.ast.Access;
import org.jetbrains.research.intellijdeodorant.core.ast.ConstructorObject;
import org.jetbrains.research.intellijdeodorant.core.ast.MethodObject;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.MethodBodyObject;

import java.util.ArrayList;
import java.util.List;

public class MethodObjectTest extends LightJavaCodeInsightFixtureTestCase {
    private static final String PATH_TO_TEST_DATA = "/core/ast/";

    private void runCheckOnFunFunctionContainsEnclosingClassAccess(@NotNull String classFileName) {
        myFixture.setTestDataPath("./src/test/resources/testdata/");
        myFixture.configureByFile(PATH_TO_TEST_DATA + classFileName);

        class Visitor extends PsiRecursiveElementVisitor {
            private List<PsiMethod> psiMethods = new ArrayList<>();

            @Override
            public void visitElement(PsiElement element) {

                if (element instanceof PsiMethod) {
                    psiMethods.add((PsiMethod) element);
                }

                super.visitElement(element);
            }

            private List<PsiMethod> getPsiMethods() {
                return psiMethods;
            }
        }

        Visitor visitor = new Visitor();

        Project project = myFixture.getProject();

        PsiFile psiFile = FilenameIndex.getFilesByName(project, classFileName, GlobalSearchScope.allScope(project))[0];

        for (int i = 0; i < psiFile.getChildren().length; i++) {
            PsiElement psiElement = psiFile.getChildren()[i];
            visitor.visitElement(psiElement);
        }

        PsiMethod psiMethod = null;

        for (int i = 0; i < visitor.getPsiMethods().size(); i++) {
            PsiMethod psiMethod1 = visitor.getPsiMethods().get(i);
            if (psiMethod1.getName().equals("fun")) {
                psiMethod = psiMethod1;
            }
        }

        final ConstructorObject constructorObject = new ConstructorObject();
        constructorObject.setMethodDeclaration(psiMethod);
        constructorObject.setName(psiMethod.getName());
        constructorObject.setClassName(psiMethod.getContainingClass().getName());
        int methodDeclarationStartPosition = psiMethod.getStartOffsetInParent();
        int methodDeclarationEndPosition = methodDeclarationStartPosition + psiMethod.getTextLength();
        constructorObject.setAccess(Access.PUBLIC);

        constructorObject.setMethodBody(new MethodBodyObject(psiMethod.getBody()));

        MethodObject methodObject = new MethodObject(psiMethod, constructorObject);

        if (methodObject.containsFieldAccessOfEnclosingClass()) {
            System.out.println("true");
        } else {
            System.out.println("false");
        }
    }

    public void testEnclosingClassOneNested() {
        runCheckOnFunFunctionContainsEnclosingClassAccess("testEnclosingClassOneNested.java");
    }

    public void testEnclosingClassTwiceNested() {
        runCheckOnFunFunctionContainsEnclosingClassAccess("testEnclosingClassTwiceNested.java");
    }

    public void testEnclosingClassStaticMember() {
        runCheckOnFunFunctionContainsEnclosingClassAccess("testEnclosingClassStaticMember.java");
    }

    public void testNotEnclosingClass() {
        runCheckOnFunFunctionContainsEnclosingClassAccess("testNotEnclosingClass.java");
    }
}