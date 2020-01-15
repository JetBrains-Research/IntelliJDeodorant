package TestConstructorCreation.actual;

import java.util.List;

public class Test {
    private TestProduct testProduct = new TestProduct();
    private int d;
    private Integer e;

    public Test(Integer a, final List<String> b, int d) {
        testProduct.setA(a);
        testProduct.setB(b);
        this.d = d;
        testProduct.setC(testProduct.getC() + 5);
    }

    public void fun1() {
        testProduct.fun1();
    }

    public void fun2() {
        d += 1;
        e += 1;
    }
}