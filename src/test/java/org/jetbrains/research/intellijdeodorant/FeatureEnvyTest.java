package org.jetbrains.research.intellijdeodorant;

import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.research.intellijdeodorant.core.FeatureEnvyVisualizationData;
import org.jetbrains.research.intellijdeodorant.core.distance.MoveMethodCandidateRefactoring;
import org.jetbrains.research.intellijdeodorant.core.distance.ProjectInfo;

import java.util.List;

public class FeatureEnvyTest extends LightJavaCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return LightJavaCodeInsightFixtureTestCase.JAVA_8;
    }

    private List<MoveMethodCandidateRefactoring> getMoveMethodCandidates(String fromClass, String toClass) {
        myFixture.addFileToProject("src/resources/A.java", fromClass);
        myFixture.addFileToProject("src/resources/B.java", toClass);
        myFixture.allowTreeAccessForAllFiles();
        Project project = myFixture.getProject();
        ProjectInfo projectInfo = new ProjectInfo(project);
        return JDeodorantFacade.getMoveMethodRefactoringOpportunities(projectInfo, new ProgressIndicatorBase());
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testCases/featureEnvy/";
    }

    public void testFieldAccessesCount() {
        String fromClass = "package testCases.featureEnvy;\n" +
                "import testCases.featureEnvy.B;\n" +
                "\n" +
                "public class A {\n" +
                "    private B bClass;\n" +
                "\n" +
                "  public void testMethod() {\n" +
                "        bClass.firstField = \"Abc\";\n" +
                "        bClass.secondField = 1;\n" +
                "  }\n" +
                "}";

        String toClass = "package testCases.featureEnvy;\n" +
                "\n" +
                "public class B {\n" +
                "\n" +
                "    public String firstField;\n" +
                "    public int secondField;\n" +
                "\n" +
                "    public void emptyMethod() {\n" +
                "    }\n" +
                "}";

        List<MoveMethodCandidateRefactoring> refactorings = getMoveMethodCandidates(fromClass, toClass);
        assertNotEmpty(refactorings);
        FeatureEnvyVisualizationData featureEnvyVisualizationData = refactorings.get(0).getVisualizationData();
        assertEquals("testMethod", featureEnvyVisualizationData.getMethodToBeMoved().getName());
        assertEquals(2, featureEnvyVisualizationData.getDistinctTargetDependencies());
        assertEquals(1, featureEnvyVisualizationData.getDistinctSourceDependencies());
    }

    public void testFieldAccessesCount2() {
        String fromClass = "package testCases.featureEnvy;\n" +
                "import testCases.featureEnvy.B;\n" +
                "\n" +
                "public class A {\n" +
                "    private B bClass;\n" +
                "\n" +
                "  public void testMethod(String testString) {\n" +
                "        bClass.firstField = testString;\n" +
                "        if (bClass.secondField == 1000) {\n" +
                "            bClass.thirdField *= 2;\n" +
                "        }" +
                "  }\n" +
                "}";

        String toClass = "package testCases.featureEnvy;\n" +
                "\n" +
                "public class B {\n" +
                "\n" +
                "    public String firstField;\n" +
                "    public int secondField;\n" +
                "    public int thirdField = 100;\n" +
                "\n" +
                "    public void emptyMethod() {\n" +
                "    }\n" +
                "}";

        List<MoveMethodCandidateRefactoring> refactorings = getMoveMethodCandidates(fromClass, toClass);
        assertNotEmpty(refactorings);
        FeatureEnvyVisualizationData featureEnvyVisualizationData = refactorings.get(0).getVisualizationData();
        assertEquals("testMethod", featureEnvyVisualizationData.getMethodToBeMoved().getName());
        assertEquals(3, featureEnvyVisualizationData.getDistinctTargetDependencies());
        assertEquals(1, featureEnvyVisualizationData.getDistinctSourceDependencies());
    }

    public void testTargetMethodInvocationsCount() {
        String fromClass = "package testCases.featureEnvy;\n" +
                "import testCases.featureEnvy.B;\n" +
                "\n" +
                "public class A {\n" +
                "\n" +
                "  public void testMethod(B bClass) {\n" +
                "        if (bClass.firstMethod()) {\n" +
                "            bClass.secondMethod();\n" +
                "            bClass.thirdMethod();\n" +
                "        }\n" +
                "  }\n" +
                "}";

        String toClass = "package testCases.featureEnvy;\n" +
                "\n" +
                "public class B {\n" +
                "\n" +
                "    public String firstField;\n" +
                "    public int secondField;\n" +
                "    public int thirdField = 100;\n" +
                "\n" +
                "    public boolean firstMethod() {\n" +
                "    return true;" +
                "    }\n" +
                "    public void secondMethod() {\n" +
                "    }\n" +
                "    public void thirdMethod() {\n" +
                "    }\n" +
                "}";

        List<MoveMethodCandidateRefactoring> refactorings = getMoveMethodCandidates(fromClass, toClass);
        assertNotEmpty(refactorings);
        FeatureEnvyVisualizationData featureEnvyVisualizationData = refactorings.get(0).getVisualizationData();
        assertEquals("testMethod", featureEnvyVisualizationData.getMethodToBeMoved().getName());
        assertEquals(3, featureEnvyVisualizationData.getDistinctTargetDependencies());
        assertEquals(0, featureEnvyVisualizationData.getDistinctSourceDependencies());
    }

    public void testTargetMethodInvocationsCount2() {
        String fromClass = "package testCases.featureEnvy;\n" +
                "import testCases.featureEnvy.B;\n" +
                "\n" +
                "public class A {\n" +
                "\n" +
                "  public boolean testMethod(B bClass) {\n" +
                "        if (bClass.getFirstField().equals(\"abc\")) {\n" +
                "            return true;\n" +
                "        }\n" +
                "        else return false;\n" +
                "  }\n" +
                "}";

        String toClass = "package testCases.featureEnvy;\n" +
                "\n" +
                "public class B {\n" +
                "\n" +
                "    public String firstField;\n" +
                "\n" +
                "    public String getFirstField() {\n" +
                "       return firstField;" +
                "    }\n" +
                "}";

        List<MoveMethodCandidateRefactoring> refactorings = getMoveMethodCandidates(fromClass, toClass);
        assertNotEmpty(refactorings);
        FeatureEnvyVisualizationData featureEnvyVisualizationData = refactorings.get(0).getVisualizationData();
        assertEquals("testMethod", featureEnvyVisualizationData.getMethodToBeMoved().getName());
        assertEquals(1, featureEnvyVisualizationData.getDistinctTargetDependencies());
        assertEquals(0, featureEnvyVisualizationData.getDistinctSourceDependencies());
    }

    public void testFieldAccessThroughParameterCount() {
        String fromClass = "package testCases.featureEnvy;\n" +
                "import testCases.featureEnvy.B;\n" +
                "\n" +
                "public class A {\n" +
                "\n" +
                "  public void testMethod(B bClass) {\n" +
                "        bClass.firstField += \"cd\";\n" +
                "    }" +
                "}";

        String toClass = "package testCases.featureEnvy;\n" +
                "\n" +
                "public class B {\n" +
                "\n" +
                "    public String firstField = \"a\";\n" +
                "}";

        List<MoveMethodCandidateRefactoring> refactorings = getMoveMethodCandidates(fromClass, toClass);
        assertNotEmpty(refactorings);
        FeatureEnvyVisualizationData featureEnvyVisualizationData = refactorings.get(0).getVisualizationData();
        assertEquals("testMethod", featureEnvyVisualizationData.getMethodToBeMoved().getName());
        assertEquals(1, featureEnvyVisualizationData.getDistinctTargetDependencies());
        assertEquals(0, featureEnvyVisualizationData.getDistinctSourceDependencies());
    }

    public void testSourceMethodInvocationsCount() {
        String fromClass = "package testCases.featureEnvy;\n" +
                "import testCases.featureEnvy.B;\n" +
                "\n" +
                "public class A {\n" +
                "\n" +
                "  public void testMethod(B bClass) {\n" +
                "       if (sourceMethod1() > 100) {" +
                "               bClass.firstField = new Date().getTime();\n" +
                "               bClass.emptyMethod();" +
                "           }\n" +
                "    }" +
                "\n" +
                "    public int sourceMethod1() {\n" +
                "       return 99;\n" +
                "    }\n" +
                "}";

        String toClass = "package testCases.featureEnvy;\n" +
                "\n" +
                "public class B {\n" +
                "\n" +
                "    public long firstField;\n" +
                "    public void emptyMethod() {" +
                "       }\n" +
                "}";

        List<MoveMethodCandidateRefactoring> refactorings = getMoveMethodCandidates(fromClass, toClass);
        assertNotEmpty(refactorings);
        FeatureEnvyVisualizationData featureEnvyVisualizationData = refactorings.get(0).getVisualizationData();
        assertEquals("testMethod", featureEnvyVisualizationData.getMethodToBeMoved().getName());
        assertEquals(2, featureEnvyVisualizationData.getDistinctTargetDependencies());
        assertEquals(1, featureEnvyVisualizationData.getDistinctSourceDependencies());
    }

}
