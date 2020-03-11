package TestChainedAccess.actual;

public class Test {
    private TestProduct testProduct = new TestProduct();
    private int a;
    private int b;
    private int c;
    private Test chained;
    private Test chained2;

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

        int[] arr = new int[this.chained.chained2.testProduct.getD()];

        test.testProduct.setD(chained.chained2.chained.testProduct.getD() + this.chained.chained2.chained.testProduct.getD());
        System.out.println(chained.testProduct.getD());
        System.out.println(chained.chained2.testProduct.getD());
        System.out.println(this.chained.testProduct.getD());
        System.out.println(this.chained.chained2.testProduct.getD());
        System.out.println(this.chained.chained2.testProduct.getD() + chained.testProduct.getD());
        System.out.println(-this.chained.chained2.testProduct.getD());
        test.testProduct.setE(this.chained.chained2.testProduct.getD() - test.chained.testProduct.getD() + 5);

        test.testProduct.setE(arr[chained.chained2.a]);
        test.testProduct.setE(testProduct.getE() + testProduct.getD() + a + b + c);

        this.chained.a = testProduct.getE();
        this.chained.chained2.chained.testProduct.setE(testProduct.getE());
        chained2.testProduct.setE(testProduct.getE());
        chained.chained2.chained.testProduct.setD(testProduct.getE());

        return test;
    }
}