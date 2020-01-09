package TestAccessOfNewObject.actual;

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

    public Test fun3() {
        Test test = new Test();
        System.out.println(test.testProduct.getD());
        test.a = a;
        test.b = b;
        test.c = c + 5;

        //This is a bug in the original plugin. Actually it gives here test.testProduct.setD(d);
        //I have no motivation to find and reproduce this bug.
        test.testProduct.setD(testProduct.getD());

        test.testProduct.setE(testProduct.getE() + testProduct.getD() + a + b + c);
        return test;
    }
}