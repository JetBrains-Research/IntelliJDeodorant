package TestInnerClassWithOuterFieldAccess.actual;

public class OuterTest {
    public static final int outer = 5;

    public class Test {
        private int a;
        private int b;
        private int c;
        private int d;
        private int e;

        public void fun1() {
            a += 1;
            b += 1;
            c += 1;
            OuterTest.outer += c + OuterTest.outer;
        }

        public void fun2() {
            d += 1;
            e += 1;
        }
    }
}