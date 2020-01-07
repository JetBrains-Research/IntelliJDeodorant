package org.jetbrains.research.intellijdeodorant.core.ast;

import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.research.intellijdeodorant.core.distance.MyAttribute;
import org.jetbrains.research.intellijdeodorant.core.distance.MyClass;
import org.jetbrains.research.intellijdeodorant.core.distance.MySystem;
import org.jetbrains.research.intellijdeodorant.core.distance.ProjectInfo;

import java.util.ArrayList;
import java.util.List;

public class FieldAccessTest extends LightJavaCodeInsightFixtureTestCase {
    private String getPrefix() {
        return "package field.accesses;\n" +
                "\n" +
                "import org.jetbrains.annotations.NotNull;\n" +
                "import org.jetbrains.annotations.Nullable;\n" +
                "\n" +
                "import java.text.NumberFormat;\n" +
                "import java.util.ArrayList;\n" +
                "import java.util.List;\n" +
                "import java.util.Objects;\n" +
                "import java.util.Random;\n" +
                "import java.util.concurrent.RejectedExecutionException;\n" +
                "import java.util.function.Function;\n" +
                "import java.util.function.Supplier;\n" +
                "\n" +
                "public class TestFieldAccess " + " {\n" +
                "    private Integer FIELD = 100;\n" +
                "    private int extraField = 5;\n" +
                "    private static final int SWITCH_CASE_TEST = 5;\n";
    }

    private String getSuffix() {
        return "\n}";
    }

    private void testMethod(String methodCode, int fieldNumber) {
        String testContent = getPrefix() + methodCode + getSuffix();

        myFixture.configureByText("TestFieldAccess.java", testContent);

        Project project = myFixture.getProject();
        ProjectInfo projectInfo = new ProjectInfo(project);

        new ASTReader(projectInfo, new ProgressIndicatorBase());
        SystemObject systemObject = ASTReader.getSystemObject();
        MySystem mySystem = new MySystem(systemObject, true);
        MyClass myClass = mySystem.getClassIterator().next();

        MyAttribute testField = myClass.getAttributeList().get(fieldNumber);
        List<String> entitySet = new ArrayList<>(testField.getFullEntitySet());

        String fieldName = "FIELD";
        if (fieldNumber == 1) {
            fieldName = "extraField";
        } else if (fieldNumber == 2) {
            fieldName = "SWITCH_CASE_TEST";
        }

        assertEquals(fieldName + "'s entity set does not contain given method.", 2, entitySet.size());
    }

    public void testSimpleTest() {
        String methodCode = " void simpleTest() {\n" +
                "        FIELD += 7;\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testBinaryExpressionTest() {
        String methodCode = "int binaryExpressionTest() {\n" +
                "        return FIELD + 6;\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testAccessWithCallingMethodTest() {
        String methodCode = "String binaryExpressionTest() {\n" +
                "        return FIELD.toString();\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testAccessWithChainCallingMethodsTest() {
        String methodCode = "int binaryExpressionTest() {\n" +
                "        return FIELD.toString().length();\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testAccessViaThisTest() {
        String methodCode = "int thisTest() {\n" +
                "        return this.FIELD;\n" +
                "    }";

        //TODO does not work access via this expression
        //testMethod(methodCode, 0);
    }


    public void testComplexExpressionTest() {
        String methodCode = "int complexExpressionTest() {\n" +
                "        return ((FIELD * 2) + (FIELD + 6)) * 6;\n" +
                "    }\n";

        testMethod(methodCode, 0);
    }

    public void testBlockStatementTest() {
        String methodCode = "void blockStatementTest() {\n" +
                "        {\n" +
                "            FIELD += 5;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testBlockStatementWithSeveralStatementsTest() {
        String methodCode = "void blockStatementWithSeveralStatementsTest() {\n" +
                "        {\n" +
                "            if (5 > 1) {\n" +
                "            }\n" +
                "            if (2 > 5) {\n" +
                "            }\n" +
                "            FIELD++;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testIfStatementConditionTest() {
        String methodCode = "void ifStatementConditionTest() {\n" +
                "        if (5 > FIELD) {\n" +
                "            extraField += 5;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testIfStatementCondition2Test() {
        String methodCode = "void ifStatementCondition2Test() {\n" +
                "        if (FIELD.equals(100)) {\n" +
                "        }\n" +
                "    }";
        testMethod(methodCode, 0);
    }

    public void testIfStatementBlockTest() {
        String methodCode = "void ifStatementBlockTest() {\n" +
                "        if (true) {\n" +
                "            FIELD += 5;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testIfStatementElseBlockTest() {
        String methodCode = "void ifStatementElseBlockTest() {\n" +
                "        if (true) {\n" +
                "        } else {\n" +
                "            FIELD += 5;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testIfStatementThenBlockTest() {
        String methodCode = "void ifStatementThenBlockTest() {\n" +
                "        if (true) {\n" +
                "            FIELD += 7;\n" +
                "        } else {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testForStatementInitiliazerTest() {
        String methodCode = "void forStatementInitiliazerTest() {\n" +
                "        for (int i = FIELD; i < 100; i++) {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testForStatementMultipleInitiliazerTest() {
        String methodCode = "void forStatementMultipleInitiliazerTest() {\n" +
                "        for (int i = 0, j = FIELD + 5, k = 5; i < 100; i++) {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testForStatementConditionTest() {
        String methodCode = "void forStatementConditionTest() {\n" +
                "        for (int i = 0; i < FIELD; i++) {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testForStatementUpdatersTest() {
        String methodCode = "void forStatementUpdatersTest() {\n" +
                "        for (int i = 0; i < 100; i += FIELD) {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testForStatementMultipleUpdatersTest() {
        String methodCode = "void forStatementMultipleUpdatersTest() {\n" +
                "        for (int i = 0, j = 5; i < 100; i += 1, j += FIELD) {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testForStatementBlockTest() {
        String methodCode = "void forStatementBlockTest() {\n" +
                "        for (int i = 0; i < 100; i++) {\n" +
                "            if (i > 6) {\n" +
                "                i += 7;\n" +
                "            }\n" +
                "\n" +
                "            i += FIELD;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testForeachEnhancedTest() {
        String methodCode = "void foreachEnhancedTest() {\n" +
                "        for (Integer i : new ArrayList<Integer>(FIELD)) {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testForeachBodyTest() {
        String methodCode = "void foreachBodyTest() {\n" +
                "        for (Integer i : new ArrayList<Integer>()) {\n" +
                "            FIELD += 5;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testWhileConditionTest() {
        String methodCode = "void whileConditionTest() {\n" +
                "        int i = 0;\n" +
                "        while (i < FIELD) {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testWhileBlockTest() {
        String methodCode = "void whileBlockTest() {\n" +
                "        while (true) {\n" +
                "            FIELD += 5;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testDoWhileConditionTest() {
        String methodCode = "void doWhileConditionTest() {\n" +
                "        int i = 0;\n" +
                "        do {\n" +
                "        } while (i < FIELD);\n" +
                "    }";

        testMethod(methodCode, 0);
    }


    public void testDoWhileBodyTest() {
        String methodCode = "void doWhileBodyTest() {\n" +
                "        do {\n" +
                "            if (5 > 2) {\n" +
                "                extraField += 5;\n" +
                "            }\n" +
                "            FIELD += 2;\n" +
                "        } while (true);\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testSwitchSwitchTest() {
        String methodCode = "void switchSwitchTest() {\n" +
                "        switch (FIELD) {\n" +
                "            default:\n" +
                "                break;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testSwitchCaseTest() {
        String methodCode = "void switchCaseTest() {\n" +
                "        int i = 100;\n" +
                "        switch (i) {\n" +
                "            case 6:\n" +
                "                break;\n" +
                "            case SWITCH_CASE_TEST:\n" +
                "                break;\n" +
                "        }\n" +
                "    }";

        //TODO PROBABLY AN INTENDED BEHAVIOUR: TEST ORIGINAL PLUGIN +
        //testMethod(methodCode, 2);
    }

    public void testSwitchCaseBodyTest() {
        String methodCode = "void switchCaseBodyTest() {\n" +
                "        int i = 100;\n" +
                "        switch (i) {\n" +
                "            case 10:\n" +
                "                break;\n" +
                "            case 100:\n" +
                "                FIELD += 10;\n" +
                "                break;\n" +
                "            case 13:\n" +
                "                break;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testAssertTest() {
        String methodCode = "void assertTest() {\n" +
                "        assert 1 < FIELD;\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testLabeledStatementTest() {
        String methodCode = "void labeledStatementTest() {\n" +
                "        test:\n" +
                "        {\n" +
                "            int i = 5;\n" +
                "            if (i > 6) {\n" +
                "                i += 1;\n" +
                "            }\n" +
                "\n" +
                "            FIELD += 6;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testReturnStatementTest() {
        String methodCode = "int returnStatementTest() {\n" +
                "        return FIELD;\n" +
                "    }";

        //TODO PROBABLY AN INTENDED MECHANIC: TEST ORIGINAL PLUGIN +
        //testMethod(methodCode, 0);
    }

    public void testReturnComplexStatementTest() {
        String methodCode = "int returnComplexStatementTest() {\n" +
                "        return this.FIELD.toString().hashCode();\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testSynchronizedSyncTest() {
        String methodCode = "void synchronizedSyncTest() {\n" +
                "        synchronized (FIELD) {\n" +
                "            FIELD += 5;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testSynchronizedBodyTest() {
        String methodCode = "void synchronizedBodyTest() {\n" +
                "        synchronized (Integer.valueOf(5)) {\n" +
                "            if (2 > 5) {\n" +
                "            }\n" +
                "\n" +
                "            FIELD += 6;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testThrowStatementTest() {
        String methodCode = "void throwStatementTest() {\n" +
                "        throw new RuntimeException(FIELD.toString());\n" +
                "    }\n";

        testMethod(methodCode, 0);
    }

    public void testTryBodyStatementTest() {
        String methodCode = "void tryBodyStatementTest() {\n" +
                "        try {\n" +
                "            if (1 > 2) {\n" +
                "            }\n" +
                "\n" +
                "            FIELD += 5;\n" +
                "        } finally {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testTryCatchBlockTest() {
        String methodCode = "void tryCatchBlockTest() {\n" +
                "        try {\n" +
                "            extraField /= 0;\n" +
                "        } catch (ArithmeticException e) {\n" +
                "            extraField += 1;\n" +
                "            FIELD += 5;\n" +
                "        }\n" +
                "    }";

        //TODO PROBABLY AN INTENDED BEHAVIOUR: TEST ORIGINAL PLUGIN +
        //testMethod(methodCode, 0);
    }

    public void testTryMultipleCatchBlockTest() {
        String methodCode = "void tryMultipleCatchBlockTest() {\n" +
                "        try {\n" +
                "            extraField /= 0;\n" +
                "        } catch (ArithmeticException e) {\n" +
                "            extraField += 1;\n" +
                "        } catch (RuntimeException e) {\n" +
                "            FIELD *= 2;\n" +
                "        } catch (StackOverflowError e) {\n" +
                "            extraField += 10;\n" +
                "        } finally {\n" +
                "            extraField -= 10;\n" +
                "        }\n" +
                "    }";

        //TODO PROBABLY AN INTENDED BEHAVIOUR: TEST ORIGINAL PLUGIN +
        //testMethod(methodCode, 0);
    }

    public void testTryFinallyBlockTest() {
        String methodCode = "void tryFinallyBlockTest() {\n" +
                "        try {\n" +
                "            extraField /= 0;\n" +
                "        } catch (ArithmeticException e) {\n" +
                "            extraField += 1;\n" +
                "        } finally {\n" +
                "            extraField += 5;\n" +
                "            FIELD += 1;\n" +
                "        }\n" +
                "    }";

        //TODO PROBABLY AN INTENDED BEHAVIOUR: TEST ORIGINAL PLUGIN +
        //testMethod(methodCode, 0);
    }

    public void testLocalVariableDeclarationTest() {
        String methodCode = "void localVariableDeclarationTest() {\n" +
                "        int a = 5;\n" +
                "        int b = FIELD;\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testLocalCLassDeclarationTest() {
        String methodCode = "void localClassDeclarationTest() {\n" +
                "        int a = 5;\n" +
                "        class LocalClass {\n" +
                "            int b = FIELD; //It will be in entity set too.\n" +
                "        }\n" +
                "    }";

        //TODO PROBABLY AN INTENDED BEHAVIOUR: TEST ORIGINAL PLUGIN +
        //testMethod(methodCode, 0);
    }

    public void testInnerClassFieldAccess() {
        String methodCode = "public static class InnerClass {\n" +
                "        void static fun() {\n" +
                "            FIELD += 5;\n" +
                "        }\n" +
                "    }";

        //TODO PROBABLY AN INTENDED BEHAVIOUR: TEST ORIGINAL PLUGIN +
        //testMethod(methodCode, 0);
    }
}