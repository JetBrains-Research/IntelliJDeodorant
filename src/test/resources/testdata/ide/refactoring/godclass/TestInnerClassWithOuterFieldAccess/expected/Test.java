package TestInnerClassWithOuterFieldAccess.actual;

public class OuterTest {
    public static final int outer = 5;

    public class Test {
        private TestProduct testProduct = new TestProduct();
        private int d;
        private int e;

        public void fun1() {
            testProduct.fun1();
        }

        public void fun2() {
            d += 1;
            e += 1;
        }
    }
}