package TestCircleDependency.actual;

public class Test {
    private TestProduct testProduct = new TestProduct();
    private int d;
    private int e;

    public void fun1() {
        testProduct.fun1(this);
    }

    public void fun2() {
        d += 1;
        e += 1;
        testProduct.fun3(this);
    }

    public void fun4() {
        e *= 2;
        testProduct.fun3(this);
    }
}