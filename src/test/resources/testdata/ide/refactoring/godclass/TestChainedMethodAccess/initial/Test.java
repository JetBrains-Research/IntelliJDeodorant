package TestChainedMethodAccess.actual;

public class Test {
    private Test chained;
    private int b;
    private int c;
    private int d;
    private int e;
    private Test chained2;

    public Test fun4() {
        chained.b += 1;
        b += 1;
        c += 1;
        return new Test();
    }

    public Test fun5() {
        d += 1;
        e += 1;

        /*
        Yet another bug in the original plugin. It gives here
        `testProduct.getChained().testProduct.setB(testProduct.getB() + 1);`

        But should be
        `testProduct.getChained().testProduct.setB(testProduct.getChained().testProduct.getB() + 1);`

        No motivation to find and reproduce this bug
         */
        chained.b += 1;

        chained.fun1().fun2();
        chained.fun1();
        chained.chained.fun1();
        chained.fun1().fun2().fun3(d);
        chained.fun1().fun2().fun3(d);
        chained.fun1().fun3(chained.b);
        chained.fun1();
        chained.chained.chained.fun1().chained.fun2().chained.fun3(chained.chained.chained.fun2().chained.b);
        chained.fun1().fun3(chained.fun3(chained.b));
        return new Test();
    }

    public int fun3(int a) {
        chained.fun1();
        this.fun1();
        this.fun1().fun2();
        this.fun1().fun2().fun3(a);
        fun1().fun2().fun3(0);
        fun2().fun3(0);
        return 1;
    }

    public Test fun1() {
        b += 1;
        c += 1;
        return new Test();
    }

    public Test fun2() {
        return new Test();
    }
}