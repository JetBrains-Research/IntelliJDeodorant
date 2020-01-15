package TestSeparateBlocks2.actual;

public class Test {
    private TestProduct testProduct = new TestProduct();
    private int a;
    private int b;
    private int c;

    public void fun1() {
        a += 1;
        b += 1;
        c += 1;
    }

    public void fun2() {
        testProduct.fun2();
    }
}