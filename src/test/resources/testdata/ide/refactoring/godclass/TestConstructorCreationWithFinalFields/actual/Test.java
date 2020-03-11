package TestConstructorCreationWithFinalFields.actual;

import java.util.List;

public class Test {
    private TestProduct testProduct;
    private final int d;
    private final Integer e;

    public Test(Integer a, final List<String> b, int d) {
        this.testProduct = new TestProduct(a, b);
        this.d = d;
        e = 6;
    }

    public void fun1() {
        testProduct.fun1();
    }

    public void fun2() {
        int d1 = d;
        int e1 = e;
        d1 += 1;
        e1 += 1;
    }
}