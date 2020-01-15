package TestRecursiveCall.actual;

public class Test {
    private TestProduct testProduct = new TestProduct();
    private int d;
    private int e;

    public void fun1(Integer arg, Integer arg2) {
        testProduct.fun1(arg, arg2);
    }

    public void fun2(Integer arg) {
        d += arg;
        e += 1;
        fun2(arg);
    }

    public void fun4() {
        e *= 2;
        testProduct.fun3(this);
    }
}