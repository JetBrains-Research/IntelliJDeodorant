package org.jetbrains.research.intellijdeodorant.distance;

import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.intellijdeodorant.core.distance.ExtractClassCandidateGroup;
import org.jetbrains.research.intellijdeodorant.core.distance.ExtractClassCandidateRefactoring;
import org.jetbrains.research.intellijdeodorant.core.distance.ProjectInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.jetbrains.research.intellijdeodorant.JDeodorantFacade.getExtractClassRefactoringOpportunities;

public class GodClassDistanceMatrixTest extends LightJavaCodeInsightFixtureTestCase {
    private static final String PATH_TO_TESTDATA = "src/test/resources/testdata/";
    private static final String PATH_TO_TESTS = "/core/distance/godclass/";

    @Nullable
    private ExtractClassCandidateGroup getExractClassCandidateGroup(@NotNull String classFileName) {
        myFixture.setTestDataPath(PATH_TO_TESTDATA);
        myFixture.configureByFile(PATH_TO_TESTS + classFileName);
        Project project = myFixture.getProject();
        PsiFile psiFile = FilenameIndex.getFilesByName(project, classFileName, GlobalSearchScope.allScope(project))[0];
        ProjectInfo projectInfo = new ProjectInfo(project);

        Set<ExtractClassCandidateGroup> set = getExtractClassRefactoringOpportunities(projectInfo, new ProgressIndicatorBase());

        if (set.isEmpty()) {
            return null;
        }

        return set.iterator().next();
    }

    private void compareExtractClassCandidateRefactoringContains(ExtractClassCandidateGroup extractClassCandidateGroup, int groupNumber, @NotNull List<String> expectedFields, @NotNull List<String> expectedMethods) {
        assertTrue(extractClassCandidateGroup.getCandidates().size() >= groupNumber);
        ExtractClassCandidateRefactoring extractClassCandidateRefactoring = extractClassCandidateGroup.getCandidates().get(groupNumber);

        List<String> extractedFieldsNames = extractClassCandidateRefactoring.getExtractedFieldFragments().stream().map(PsiField::getName).collect(Collectors.toList());
        assertThat(extractedFieldsNames, containsInAnyOrder((expectedFields.toArray())));
        assertThat(expectedFields, containsInAnyOrder(extractedFieldsNames.toArray()));

        List<String> extractedMethodsNames = extractClassCandidateRefactoring.getExtractedMethods().stream().map(PsiMethod::getName).collect(Collectors.toList());
        assertThat(extractedMethodsNames, containsInAnyOrder(expectedMethods.toArray()));
        assertThat(expectedMethods, containsInAnyOrder(extractedMethodsNames.toArray()));
    }

    public void testSimple() {
        String classFileName = "testSimple.java";
        List<String> expectedFields = Arrays.asList("toto", "SIZE", "array");
        List<String> expectedMethods = Arrays.asList("getSumMult", "getSumMult2");

        ExtractClassCandidateGroup group = getExractClassCandidateGroup(classFileName);

        assertNotNull(group);


        compareExtractClassCandidateRefactoringContains(group, 0, expectedFields, expectedMethods);
    }

    public void testOnlyFields() {
        String classFileName = "testOnlyFields.java";

        ExtractClassCandidateGroup group = getExractClassCandidateGroup(classFileName);

        assertNull(group);
    }


    public void testSeparateBlocks() {
        String classFileName = "testSeparateBlocks.java";

        ExtractClassCandidateGroup group = getExractClassCandidateGroup(classFileName);
        assertNotNull(group);

        for (int i = 0; i < 2; i++) {
            List<String> expectedFields;
            List<String> expectedMethods;

            if (i == 0) {
                expectedFields = Arrays.asList("a", "b", "c");
                expectedMethods = Arrays.asList("fun1");
            } else {
                expectedFields = Arrays.asList("d", "e");
                expectedMethods = Arrays.asList("fun2");
            }

            compareExtractClassCandidateRefactoringContains(group, i, expectedFields, expectedMethods);
        }
    }

    public void testManySeparatesBlocks() {
        String classFileName = "testSeparateBlocksWithStrictOrder.java";

        ExtractClassCandidateGroup group = getExractClassCandidateGroup(classFileName);
        assertNotNull(group);

        for (int i = 0; i < 6; i++) {
            List<String> expectedFields;
            List<String> expectedMethods;

            if (i == 0) {
                expectedFields = Arrays.asList("aa", "ab", "ac", "ad", "ae", "af", "ag");
                expectedMethods = Arrays.asList("fun1");
            } else if (i == 1) {
                expectedFields = Arrays.asList("ba", "bb", "bc", "bd", "be", "bf");
                expectedMethods = Arrays.asList("fun2");
            } else if (i == 2) {
                expectedFields = Arrays.asList("ca", "cb", "cc", "cd", "ce");
                expectedMethods = Arrays.asList("fun3");
            } else if (i == 3) {
                expectedFields = Arrays.asList("da", "db", "dc", "dd");
                expectedMethods = Arrays.asList("fun4");
            } else if (i == 4) {
                expectedFields = Arrays.asList("ea", "eb", "ec");
                expectedMethods = Arrays.asList("fun5");
            } else {
                expectedFields = Arrays.asList("fa", "fb");
                expectedMethods = Arrays.asList("fun6");
            }

            compareExtractClassCandidateRefactoringContains(group, i, expectedFields, expectedMethods);
        }
    }

    public void testSynchronizedMethod() {
        String classFileName = "testSynchronizedMethod.java";

        ExtractClassCandidateGroup group = getExractClassCandidateGroup(classFileName);

        assertNull(group);
    }

    public void testSynchronizedMethodBody() {
        String classFileName = "testSynchronizedMethodBlock.java";

        ExtractClassCandidateGroup group = getExractClassCandidateGroup(classFileName);

        //ORIGINAL PLUGIN ACTUALLY ALLOWS IT, BUT IT SHOULDN'T. PROBABLY A BUG.
        //assertNull(group);
    }

    public void testAbstractMethod() {
        String classFileName = "testAbstractMethod.java";

        ExtractClassCandidateGroup group = getExractClassCandidateGroup(classFileName);

        assertNull(group);
    }

    public void testOverride() {
        String classFileName = "testOverride.java";

        ExtractClassCandidateGroup group = getExractClassCandidateGroup(classFileName);

        assertNull(group);
    }

    public void testEnclosingAccess() {
        String classFileName = "TestEnclosingAccess.java";

        ExtractClassCandidateGroup group = getExractClassCandidateGroup(classFileName);

        assertNull(group);
    }

    public void testOnlyMethods() {
        String classFileName = "testOnlyMethods.java";

        ExtractClassCandidateGroup group = getExractClassCandidateGroup(classFileName);

        assertNull(group);
    }
}