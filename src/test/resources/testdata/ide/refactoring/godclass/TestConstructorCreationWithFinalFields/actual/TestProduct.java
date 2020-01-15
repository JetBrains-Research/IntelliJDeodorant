package TestConstructorCreationWithFinalFields.actual;

import java.util.List;

public class TestProduct {
    private final Integer a;
    private final List<String> b;
    private final Integer c = 6;

    public TestProduct(Integer a, List<String> b) {
        this.a = a;
        this.b = b;
    }

    public void fun1() {
        int a1 = 0;
        a1 += a;
        List<String> b1;
        b1 = b;
        b1.add("lol");

        int c1 = 5;
        c1 += c;
    }
}