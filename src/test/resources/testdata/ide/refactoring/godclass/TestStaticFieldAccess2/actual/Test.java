package TestStaticFieldAccess2.actual;

public class Test {
    private TestProduct testProduct = new TestProduct();
    public static Integer b = 2;
    private int d = 4;
    private int e = 5;

    public Test(int val) {
        b = val;
        testProduct.setA(testProduct.getA() + b);
        b = testProduct.getA();
    }

    public void fun1() {

        testProduct.fun1(this);
    }

    public void fun2(int t) {
        d += t;
        e += t;
    }
}