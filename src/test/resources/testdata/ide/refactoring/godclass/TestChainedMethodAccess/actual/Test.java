package TestChainedMethodAccess.actual;

public class Test {
    private TestProduct testProduct = new TestProduct();
    private int d;
    private int e;
    private Test chained2;

    public Test fun4() {
        return testProduct.fun4();
    }

    public Test fun5() {
        d += 1;
        e += 1;

        /*
        Bug in the original plugin. It gives here
        `testProduct.getChained().testProduct.setB(testProduct.getB() + 1);`

        But should be
        `testProduct.getChained().testProduct.setB(testProduct.getChained().testProduct.getB() + 1);`
         */
        testProduct.getChained().testProduct.setB(testProduct.getChained().testProduct.getB() + 1);

        testProduct.getChained().fun1().fun2();
        testProduct.getChained().fun1();
        testProduct.getChained().testProduct.getChained().fun1();
        testProduct.getChained().fun1().fun2().fun3(d);
        testProduct.getChained().fun1().fun2().fun3(d);
        testProduct.getChained().fun1().fun3(testProduct.getChained().testProduct.getB());
        testProduct.getChained().fun1();
        testProduct.getChained().testProduct.getChained().testProduct.getChained().fun1().testProduct.getChained().fun2().testProduct.getChained().fun3(testProduct.getChained().testProduct.getChained().testProduct.getChained().fun2().testProduct.getChained().testProduct.getB());
        testProduct.getChained().fun1().fun3(testProduct.getChained().fun3(testProduct.getChained().testProduct.getB()));
        return new Test();
    }

    public int fun3(int a) {
        testProduct.getChained().fun1();
        testProduct.fun1();
        testProduct.fun1().fun2();
        testProduct.fun1().fun2().fun3(a);
        testProduct.fun1().fun2().fun3(0);
        fun2().fun3(0);
        return 1;
    }

    public Test fun1() {
        return testProduct.fun1();
    }

    public Test fun2() {
        return new Test();
    }
}