package TestConstructorCreationWithFinalFields.actual;

import java.util.List;

public class Test {
    private final Integer a;
    private final List<String> b;
    private final Integer c = 6;
    private final int d;
    private final Integer e;

    public Test(Integer a, final List<String> b, int d) {
        this.a = a;
        this.b = b;
        this.d = d;
        e = 6;
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

    public void fun2() {
        int d1 = d;
        int e1 = e;
        d1 += 1;
        e1 += 1;
    }
}