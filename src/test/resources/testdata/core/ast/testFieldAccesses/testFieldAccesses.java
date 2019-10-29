package field.accesses;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

public class FieldAccesses {
    private Integer SIZE = 100;
    private int extraField = 5;
    private static final int SWITCH_CASE_TEST = 5;

    public class InnerClassTest {
        void simpleTest() {
            SIZE += 7;
        }

        int binaryExpressionTest() {
            return SIZE + 6;
        }

        int complexExpressionTest() {
            return ((SIZE * 2) + (SIZE + 6)) * 6;
        }

        void blockStatementTest() {
            {
                SIZE += 5;
            }
        }

        void blockStatementWithSeveralStatementsTest() {
            {
                if (5 > 1) {
                }
                if (2 > 5) {
                }
                SIZE++;
            }
        }

        void ifStatementConditionTest() {
            if (5 > SIZE) {
                extraField += 5;
            }
        }

        void ifStatementCondition2Test() {
            if (SIZE.equals(100)) {
            }
        }

        void ifStatementBlockTest() {
            if (true) {
                SIZE += 5;
            }
        }

        void ifStatementElseBlockTest() {
            if (true) {
            } else {
                SIZE += 5;
            }
        }

        void ifStatementThenBlockTest() {
            if (true) {
                SIZE += 7;
            } else {
            }
        }

        void forStatementInitiliazerTest() {
            for (int i = SIZE; i < 100; i++) {
            }
        }

        void forStatementMultipleInitiliazerTest() {
            for (int i = 0, j = SIZE, k = 5; i < 100; i++) {
            }
        }

        void forStatementConditionTest() {
            for (int i = 0; i < SIZE; i++) {
            }
        }

        void forStatementUpdatersTest() {
            for (int i = 0; i < 100; i += SIZE) {
            }
        }

        void forStatementMultipleUpdatersTest() {
            for (int i = 0, j = 5; i < 100; i += 1, j += SIZE) {
            }
        }

        void forStatementBlockTest() {
            for (int i = 0; i < 100; i++) {
                if (i > 6) {
                    i += 7;
                }

                i += SIZE;
            }
        }

        void foreachEnhancedTest() {
            for (Integer i : new ArrayList<Integer>(SIZE)) {
            }
        }

        void foreachBodyTest() {
            for (Integer i : new ArrayList<Integer>()) {
                SIZE += 5;
            }
        }

        void whileConditionTest() {
            int i = 0;
            while (i < SIZE) {
            }
        }

        void whileBlockTest() {
            while (true) {
                SIZE += 5;
            }
        }

        void doWhileConditionTest() {
            int i = 0;
            do {
            } while (i < SIZE);
        }

        void doWhileBodyTest() {
            do {
                if (5 > 2) {
                    extraField += 5;
                }
                SIZE += 2;
            } while (true);
        }

        void switchSwitchTest() {
            switch (SIZE) {
                default:
                    break;
            }
        }

        void switchCaseTest() {
            int i = 100;
            switch (i) {
                case 6:
                    break;
                case SWITCH_CASE_TEST:
                    break;
            }
        }

        void switchCaseBodyTest() {
            int i = 100;
            switch (i) {
                case 10:
                    break;
                case 100:
                    SIZE += 10;
                    break;
                case 13:
                    break;
            }
        }

        void assertTest() {
            assert 1 < SIZE;
        }

        void labeledStatementTest() {
            test:
            {
                int i = 5;
                if (i > 6) {
                    i += 1;
                }

                SIZE += 6;
            }
        }

        int returnStatementTest() {
            return SIZE;
        }

        int returnComplexStatementTest() {
            return FieldAccesses.this.SIZE.toString().hashCode();
        }

        void synzhronizedSyncTest() {
            synchronized (SIZE) {
                SIZE += 5;
            }
        }

        void synzhronizedBodyTest() {
            synchronized (Integer.valueOf(5)) {
                if (2 > 5) {
                }

                SIZE += 6;
            }
        }

        void throwStatementTest() {
            throw new RuntimeException(SIZE.toString());
        }

        void tryBodyStatementTest() {
            try {
                if (1 > 2) {
                }

                SIZE += 5;
            } finally {
            }
        }

        void tryCatchBlockTest() {
            try {
                extraField /= 0;
            } catch (ArithmeticException e) {
                extraField += 1;
                SIZE += 5;
            }
        }

        void tryMultipleCatchBlockTest() {
            try {
                extraField /= 0;
            } catch (ArithmeticException e) {
                extraField += 1;
            } catch (RuntimeException e) {
                SIZE *= 2;
            } catch (StackOverflowError e) {
                extraField += 10;
            } finally {
                extraField -= 10;
            }
        }

        void tryFinallyBlockTest() {
            try {
                extraField /= 0;
            } catch (ArithmeticException e) {
                extraField += 1;
            } finally {
                extraField += 5;
                SIZE += 1;
            }
        }

        void localVariableDeclarationTest() {
            int a = 5;
            int b = SIZE;
        }

        void localCLassDeclarationTest() {
            int a = 5;
            class LocalClass {
                int b = SIZE; //It will be in entity set too.
            }
        }
    }
}