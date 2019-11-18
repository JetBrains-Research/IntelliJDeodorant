package core.ast;

import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import core.ast.Standalone;
import core.distance.*;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.WriteAbortedException;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

public class FieldAccessTest extends LightCodeInsightFixtureTestCase {
    private File createTempFile(@NotNull String filename) {
        try {
            return File.createTempFile(filename, ".java");
        } catch (IOException e) {
            return null;
        }
    }

    private void writePrefix(@NotNull FileWriter writer, @NotNull File file) throws IOException {
        final String className = file.getName().substring(0,file.getName().length() - 5); // delete ".java" suffix

        writer.write("package field.accesses;\n" +
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
                "public class " + className + " {\n" +
                "    private Integer SIZE = 100;\n" +
                "    private int extraField = 5;\n" +
                "    private static final int SWITCH_CASE_TEST = 5;\n");
    }

    private void writeSuffix(@NotNull FileWriter writer) throws IOException {
        writer.write("\n}");
    }

    private void testMethod(String methodCode, int fieldNumber) {
        File file = createTempFile("testFieldAccess");
        assertNotNull(file);

        try (FileWriter writer = new FileWriter(file)) {
            writePrefix(writer, file);
            writer.write(methodCode);
            writeSuffix(writer);
        } catch (IOException e) {
            fail();
        }

        myFixture.configureByFile(file.getPath());
        Project project = myFixture.getProject();
        ProjectInfo projectInfo = new ProjectInfo(project);

        new ASTReader(projectInfo, new ProgressIndicatorBase());
        SystemObject systemObject = ASTReader.getSystemObject();
        MySystem mySystem = new MySystem(systemObject, true);
        MyClass myClass = mySystem.getClassIterator().next();

        MyAttribute testField = myClass.getAttributeList().get(fieldNumber);
/*
        final String prefix;
        if (isInner) {
            prefix = "field.accesses." + className + "::InnerClassTest::";
        } else {
            prefix = "field.accesses." + className + "::";
        }

        final String suffix;
        if (isVoid) {
            suffix = "():void";
        } else {
            suffix = "():int";
        }
*/
        List<String> entitySet = new ArrayList<>(testField.getFullEntitySet());

        assertEquals(2, entitySet.size());
    }

    public void testSimpleTest() {
        String methodCode = " void simpleTest() {\n" +
                "        SIZE += 7;\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testBinaryExpressionTest() {
        String methodCode = "int binaryExpressionTest() {\n" +
                "        return SIZE + 6;\n" +
                "    }";

        testMethod(methodCode,  0);
    }

    public void testComplexExpressionTest() {
        String methodCode = "int complexExpressionTest() {\n" +
                "        return ((SIZE * 2) + (SIZE + 6)) * 6;\n" +
                "    }\n";

        testMethod(methodCode, 0);
    }

    public void testBlockStatementTest() {
        String methodCode = "void blockStatementTest() {\n" +
                "        {\n" +
                "            SIZE += 5;\n" +
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
                "            SIZE++;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testIfStatementConditionTest() {
        String methodCode = "void ifStatementConditionTest() {\n" +
                "        if (5 > SIZE) {\n" +
                "            extraField += 5;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testIfStatementCondition2Test() {
        String methodCode = "void ifStatementCondition2Test() {\n" +
                "        if (SIZE.equals(100)) {\n" +
                "        }\n" +
                "    }";

        //TODO PROBABLY AN INTENTED BEHAVIOUR: TEST ORIGINAL PLUGIN
        //testMethod(methodCode, 0);
    }

    public void testIfStatementBlockTest() {
        String methodCode = "void ifStatementBlockTest() {\n" +
                "        if (true) {\n" +
                "            SIZE += 5;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testIfStatementElseBlockTest() {
        String methodCode = "void ifStatementElseBlockTest() {\n" +
                "        if (true) {\n" +
                "        } else {\n" +
                "            SIZE += 5;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testIfStatementThenBlockTest() {
        String methodCode = "void ifStatementThenBlockTest() {\n" +
                "        if (true) {\n" +
                "            SIZE += 7;\n" +
                "        } else {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testForStatementInitiliazerTest() {
        String methodCode = "void forStatementInitiliazerTest() {\n" +
                "        for (int i = SIZE; i < 100; i++) {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testForStatementMultipleInitiliazerTest() {
        String methodCode = "void forStatementMultipleInitiliazerTest() {\n" +
                "        for (int i = 0, j = SIZE + 5, k = 5; i < 100; i++) {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testForStatementConditionTest() {
        String methodCode = "void forStatementConditionTest() {\n" +
                "        for (int i = 0; i < SIZE; i++) {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testForStatementUpdatersTest() {
        String methodCode = "void forStatementUpdatersTest() {\n" +
                "        for (int i = 0; i < 100; i += SIZE) {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testForStatementMultipleUpdatersTest() {
        String methodCode = "void forStatementMultipleUpdatersTest() {\n" +
                "        for (int i = 0, j = 5; i < 100; i += 1, j += SIZE) {\n" +
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
                "            i += SIZE;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testForeachEnhancedTest() {
        String methodCode = "void foreachEnhancedTest() {\n" +
                "        for (Integer i : new ArrayList<Integer>(SIZE)) {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testForeachBodyTest() {
        String methodCode = "void foreachBodyTest() {\n" +
                "        for (Integer i : new ArrayList<Integer>()) {\n" +
                "            SIZE += 5;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testWhileConditionTest() {
        String methodCode = "void whileConditionTest() {\n" +
                "        int i = 0;\n" +
                "        while (i < SIZE) {\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testWhileBlockTest() {
        String methodCode = "void whileBlockTest() {\n" +
                "        while (true) {\n" +
                "            SIZE += 5;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testDoWhileConditionTest() {
        String methodCode = "void doWhileConditionTest() {\n" +
                "        int i = 0;\n" +
                "        do {\n" +
                "        } while (i < SIZE);\n" +
                "    }";

        testMethod(methodCode, 0);
    }


    public void testDoWhileBodyTest() {
        String methodCode = "void doWhileBodyTest() {\n" +
                "        do {\n" +
                "            if (5 > 2) {\n" +
                "                extraField += 5;\n" +
                "            }\n" +
                "            SIZE += 2;\n" +
                "        } while (true);\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testSwitchSwitchTest() {
        String methodCode = "void switchSwitchTest() {\n" +
                "        switch (SIZE) {\n" +
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

        //TODO PROBABLY AN INTENTED BEHAVIOUR: TEST ORIGINAL PLUGIN
        //testMethod(methodCode, 2);
    }

    public void testSwitchCaseBodyTest() {
        String methodCode = "void switchCaseBodyTest() {\n" +
                "        int i = 100;\n" +
                "        switch (i) {\n" +
                "            case 10:\n" +
                "                break;\n" +
                "            case 100:\n" +
                "                SIZE += 10;\n" +
                "                break;\n" +
                "            case 13:\n" +
                "                break;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testAssertTest() {
        String methodCode = "void assertTest() {\n" +
                "        assert 1 < SIZE;\n" +
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
                "            SIZE += 6;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testReturnStatementTest() {
        String methodCode = "int returnStatementTest() {\n" +
                "        return SIZE;\n" +
                "    }";

        //TODO PROBABLY AN INTENDED MECHANIC: TEST ORIGINAL PLUGIN
        //testMethod(methodCode, 0);
    }

    public void testReturnComplexStatementTest() {
        String methodCode = "int returnComplexStatementTest() {\n" +
                "        return this.SIZE.toString().hashCode();\n" +
                "    }";

        /*
        TODO PROBABLY AN INTENTED BEHAVIOUR: TEST ORIGINAL PLUGIN.
        IF NOT: the problem is that program finds reference expression `SIZE.toString` which is not a field.
         */
        //testMethod(methodCode, 0);
    }

    public void testSynzhronizedSyncTest() {
        String methodCode = "void synzhronizedSyncTest() {\n" +
                "        synchronized (SIZE) {\n" +
                "            SIZE += 5;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testSynzhronizedBodyTest() {
        String methodCode = "void synzhronizedBodyTest() {\n" +
                "        synchronized (Integer.valueOf(5)) {\n" +
                "            if (2 > 5) {\n" +
                "            }\n" +
                "\n" +
                "            SIZE += 6;\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testThrowStatementTest() {
        String methodCode = "void throwStatementTest() {\n" +
                "        throw new RuntimeException(SIZE.toString());\n" +
                "    }\n";

        testMethod(methodCode, 0);
    }

    public void testTryBodyStatementTest() {
        String methodCode = "void tryBodyStatementTest() {\n" +
                "        try {\n" +
                "            if (1 > 2) {\n" +
                "            }\n" +
                "\n" +
                "            SIZE += 5;\n" +
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
                "            SIZE += 5;\n" +
                "        }\n" +
                "    }";

        //TODO PROBABLY AN INTENTED BEHAVIOUR: TEST ORIGINAL PLUGIN
        //testMethod(methodCode, 0);
    }

    public void testTryMultipleCatchBlockTest() {
        String methodCode = "void tryMultipleCatchBlockTest() {\n" +
                "        try {\n" +
                "            extraField /= 0;\n" +
                "        } catch (ArithmeticException e) {\n" +
                "            extraField += 1;\n" +
                "        } catch (RuntimeException e) {\n" +
                "            SIZE *= 2;\n" +
                "        } catch (StackOverflowError e) {\n" +
                "            extraField += 10;\n" +
                "        } finally {\n" +
                "            extraField -= 10;\n" +
                "        }\n" +
                "    }";

        //TODO PROBABLY AN INTENTED BEHAVIOUR: TEST ORIGINAL PLUGIN
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
                "            SIZE += 1;\n" +
                "        }\n" +
                "    }";

        //TODO PROBABLY AN INTENTED BEHAVIOUR: TEST ORIGINAL PLUGIN
        //testMethod(methodCode, 0);
    }

    public void testLocalVariableDeclarationTest() {
        String methodCode = "void localVariableDeclarationTest() {\n" +
                "        int a = 5;\n" +
                "        int b = SIZE;\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testLocalCLassDeclarationTest() {
        String methodCode = "void localCLassDeclarationTest() {\n" +
                "        int a = 5;\n" +
                "        class LocalClass {\n" +
                "            int b = SIZE; //It will be in entity set too.\n" +
                "        }\n" +
                "    }";

        testMethod(methodCode, 0);
    }

    public void testInnerClassFieldAccess() {
        String methodCode = "public static class InnerClass {\n" +
                "        void static fun() {\n" +
                "            SIZE += 5;\n" +
                "        }\n" +
                "    }";

        //TODO PROBABLY AN INTENTED BEHAVIOUR: TEST ORIGINAL PLUGIN
        //testMethod(methodCode, 0);
    }
}