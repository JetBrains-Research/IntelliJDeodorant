package TestClone.actual;

public class Test implements Cloneable {
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

    public Test clone() {
        Test test = new Test();
        test.a = a;
        test.b = b;
        test.c = c + 5;

        //Original plugin gives here `test.testProduct.setD(d);` This is a bug.
        test.testProduct.setD(testProduct.getD());

        test.testProduct.setE(testProduct.getE() + testProduct.getD() + a + b + c);
        return test;
    }
}