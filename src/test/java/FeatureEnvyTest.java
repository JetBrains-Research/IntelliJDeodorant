import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;

import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import core.FeatureEnvyVisualizationData;
import core.ast.Standalone;
import core.distance.MoveMethodCandidateRefactoring;
import core.distance.ProjectInfo;
import org.jetbrains.annotations.NotNull;

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
        return Standalone.getMoveMethodRefactoringOpportunities(projectInfo, new ProgressIndicatorBase());
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testCases/featureEnvy/";
    }

    public void testSearchOfDependencies() {
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

    public void testSearchOfDependencies2() {
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

}
