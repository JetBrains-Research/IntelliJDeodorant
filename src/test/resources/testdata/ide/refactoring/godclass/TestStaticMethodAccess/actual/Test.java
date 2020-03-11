package TestStaticMethodAccess.actual;

public class Test {
    private TestProduct testProduct = new TestProduct();
    private int a = 1;
    private static Integer b = 2;
    private int c = 3;

    public Test(int val) {
        b = val;
        a += b;
        b = a;
    }

    public void fun1() {
        a += b;
        b = (a + b + c) * (a + b);
        c = 1;
        c = (b + b + b);

        System.out.print(b);
        testProduct.fun2(b);
        fun3();
        fun4();
    }

    public void fun2(int t) {
        testProduct.fun2(t);
    }

    public static void fun3() {
        fun4();
    }

    private static void fun4() {
        fun3();
    }

    public static void fun5() {
        fun6();
    }

    private static void fun6() {
    }
}