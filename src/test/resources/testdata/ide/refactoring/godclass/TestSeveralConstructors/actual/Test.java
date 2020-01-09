package SeveralConstructors.actual;

import java.util.List;

public class Test {
    private TestProduct testProduct = new TestProduct();
    private int d;
    private Integer e;

    public Test(Integer a, List<String> b, int d) {
        testProduct.setA(a);
        testProduct.setB(b);
        this.d = d;
        e = 6;
    }

    public Test(Integer a, List<String> b) {
        testProduct.setA(a);
        testProduct.setB(b);
    }

    public Test(int field) {
        testProduct.setA(field);
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