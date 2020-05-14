package TestFinalInitialisationWithLocalVariable.actual;

public class Test {
    private TestProduct testProduct;
    private final int init = 5;

    private int a;
    private int b;
    private int c;
    public Test() {
        this.testProduct = new TestProduct(init);

        a = 5;
        b = 5;
        c = 5;
        testProduct.setE(5);
    }

    public void fun1() {
        System.out.print(a);
        System.out.print(b);
        System.out.print(c);
    }

    public void fun2() {
        testProduct.fun2();
    }

    public void fun3() {
        System.out.print(a);
        System.out.print(b);
        System.out.print(c);
        System.out.print(testProduct.getD());
        System.out.print(testProduct.getE());
    }
}