package core.ast;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import core.distance.ProjectInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import refactoring.*;
import ui.functionalinterfaces.QuadriFunction;

import java.util.Set;

public class TypeStateCheckingTest extends LightJavaCodeInsightFixtureTestCase {
    private static final String TEST_ROOT = "src/test/resources/testdata/core/ast/typestatechecking/";
    private ProgressIndicator fakeProgressIndicator = new FakeProgressIndicator();

    public void testAdditionalConstants() {
        performSingleRefactoringTest();
    }

    public void testExceptionInSignature() {
        performTwoRefactoringsTest();
    }

    public void testExistingGetterSetter() {
        performSingleRefactoringTest();
    }

    public void testGetClass() {
        performSingleRefactoringTest();
    }

    public void testImports() {
        performTwoRefactoringsTest();
    }

    public void testInstanceof() {
        performSingleRefactoringTest();
    }

    public void testNestedEnum() {
        performSingleRefactoringTest();
    }

    public void testOuterLocalVariables() {
        performTwoRefactoringsTest();
    }

    public void testSeparateFileEnum() {
        performSingleRefactoringTest();
    }

    public void testStateField() {
        performSingleRefactoringTest();
    }

    public void testStateFieldAccesses() {
        performSingleRefactoringTest();
    }

    public void testStateLocalVariable() {
        performSingleRefactoringTest();
    }

    public void testStateParameter() {
        performSingleRefactoringTest();
    }

    public void testSwitchOperator() {
        performSingleRefactoringTest();
    }

    public void testTwoTypeCheckingFragments() {
        performTwoRefactoringsTest();
    }

    private void performTwoRefactoringsTest() {
        initTest();
        Project project = myFixture.getProject();
        for (int i = 0; i < 2; i++) {
            Set<TypeCheckEliminationGroup> set = Standalone.getTypeCheckEliminationRefactoringOpportunities(
                    new ProjectInfo(project), fakeProgressIndicator
            );
            assertEquals(set.size(), 2 - i);
            TypeCheckEliminationGroup eliminationGroup = set.iterator().next();
            assertEquals(eliminationGroup.getCandidates().size(), 1);
            WriteCommandAction.runWriteCommandAction(
                    project,
                    () -> createRefactoring(eliminationGroup.getCandidates().get(0), project).apply()
            );
        }
        checkTest();
    }

    private void performSingleRefactoringTest() {
        initTest();
        Project project = myFixture.getProject();
        Set<TypeCheckEliminationGroup> set = Standalone.getTypeCheckEliminationRefactoringOpportunities(
                new ProjectInfo(project), fakeProgressIndicator
        );
        assertEquals(set.size(), 1);
        TypeCheckEliminationGroup eliminationGroup = set.iterator().next();
        assertEquals(eliminationGroup.getCandidates().size(), 1);
        WriteCommandAction.runWriteCommandAction(
                project,
                () -> createRefactoring(eliminationGroup.getCandidates().get(0), project).apply()
        );
        checkTest();
    }

    @Override
    protected String getTestDataPath() {
        return TEST_ROOT + getTestName(true);
    }

    private void initTest() {
        myFixture.copyDirectoryToProject("initial", "");
    }

    private void checkDirectoriesAreEqual(PsiDirectory result, PsiDirectory expected) {
        for (PsiDirectory directory : result.getSubdirectories()) {
            if (directory.getName().equals("expected")) {
                continue;
            }
            checkDirectoriesAreEqual(directory, expected.findSubdirectory(directory.getName()));
        }
        for (PsiFile file : result.getFiles()) {
            checkFilesAreEqual(file, expected.findFile(file.getName()));
        }
    }

    private void checkFilesAreEqual(PsiFile result, PsiFile expected) {
        String[] resultTokens = result.getText().trim().split("\\s+");
        String[] expectedTokens = expected.getText().trim().split("\\s+");
        assertOrderedEquals(resultTokens, expectedTokens);
    }

    private void checkTest() {
        myFixture.copyDirectoryToProject("expected", "expected");
        VirtualFile directoryFile = myFixture.findFileInTempDir("");
        PsiDirectory mainDirectory = myFixture.getPsiManager().findDirectory(directoryFile);
        PsiDirectory expectedDirectory = mainDirectory.findSubdirectory("expected");
        checkDirectoriesAreEqual(mainDirectory, expectedDirectory);
    }

    private static PolymorphismRefactoring createRefactoring(TypeCheckElimination typeCheckElimination,
                                                             Project project) {
        PsiClass sourceTypeDeclaration = typeCheckElimination.getTypeCheckClass();
        PsiFile sourceFile = sourceTypeDeclaration.getContainingFile();
        QuadriFunction<PsiFile, Project, PsiClass, TypeCheckElimination, PolymorphismRefactoring> constructor;
        if (typeCheckElimination.getExistingInheritanceTree() == null) {
            constructor = ReplaceTypeCodeWithStateStrategy::new;
        } else {
            constructor = ReplaceConditionalWithPolymorphism::new;
        }

        return constructor.apply(sourceFile, project, sourceTypeDeclaration, typeCheckElimination);
    }

    private static class FakeProgressIndicator implements ProgressIndicator {
        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public void cancel() {
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void setText(String text) {
        }

        @Override
        public String getText() {
            return null;
        }

        @Override
        public void setText2(String text) {

        }

        @Override
        public String getText2() {
            return null;
        }

        @Override
        public double getFraction() {
            return 0;
        }

        @Override
        public void setFraction(double fraction) {
        }

        @Override
        public void pushState() {
        }

        @Override
        public void popState() {
        }

        @Override
        public boolean isModal() {
            return false;
        }

        @NotNull
        @Override
        public ModalityState getModalityState() {
            return null;
        }

        @Override
        public void setModalityProgress(@Nullable ProgressIndicator modalityProgress) {
        }

        @Override
        public boolean isIndeterminate() {
            return false;
        }

        @Override
        public void setIndeterminate(boolean indeterminate) {
        }

        @Override
        public void checkCanceled() throws ProcessCanceledException {
        }

        @Override
        public boolean isPopupWasShown() {
            return false;
        }

        @Override
        public boolean isShowing() {
            return false;
        }
    }
}