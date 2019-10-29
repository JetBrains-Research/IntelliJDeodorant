package core.distance;

import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import core.ast.Standalone;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class GodClassDistanceMatrixTest extends LightCodeInsightFixtureTestCase {
    private static final String PATH_TO_TEST_DATA = "src/test/resources/testdata/core/distance/godclass/";

    private ExtractClassCandidateGroup getExractClassCandidateGroup(@NotNull String classFileName) {
        myFixture.configureByFile(PATH_TO_TEST_DATA + classFileName);
        Project project = myFixture.getProject();
        PsiFile psiFile = FilenameIndex.getFilesByName(project, classFileName, GlobalSearchScope.allScope(project))[0];
        ProjectInfo projectInfo = new ProjectInfo(project);

        Set<ExtractClassCandidateGroup> set = Standalone.getExtractClassRefactoringOpportunities(projectInfo, new ProgressIndicatorBase());

        assertTrue(set.iterator().hasNext());
        return set.iterator().next();
    }

    private void compareExtractClassCandidateRefactoringContains(ExtractClassCandidateGroup extractClassCandidateGroup, int groupNumber, @NotNull List<String> expectedFields, @NotNull List<String> expectedMethods) {
        assertTrue(extractClassCandidateGroup.getCandidates().size() >= groupNumber);
        ExtractClassCandidateRefactoring extractClassCandidateRefactoring = extractClassCandidateGroup.getCandidates().get(groupNumber);

        List<String> extractedFieldsNames = extractClassCandidateRefactoring.getExtractedFieldFragments().stream().map(PsiField::getName).collect(Collectors.toList());
        assertContainsElements(extractedFieldsNames, expectedFields);
        assertContainsElements(expectedFields, extractedFieldsNames);

        List<String> extractedMethodsNames = extractClassCandidateRefactoring.getExtractedMethods().stream().map(PsiMethod::getName).collect(Collectors.toList());
        assertContainsElements(extractedMethodsNames, expectedMethods);
        assertContainsElements(expectedMethods, extractedMethodsNames);
    }

    public void testSimple() {
        String classFileName = "testSimple.java";
        List<String> expectedFields = Arrays.asList("toto", "SIZE", "array");
        List<String> expectedMethods = Arrays.asList("getSumMult", "getSumMult2");

        ExtractClassCandidateGroup group = getExractClassCandidateGroup(classFileName);

        compareExtractClassCandidateRefactoringContains(group, 0, expectedFields, expectedMethods);
    }
}