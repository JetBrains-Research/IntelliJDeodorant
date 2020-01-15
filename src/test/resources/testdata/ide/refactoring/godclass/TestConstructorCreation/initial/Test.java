package TestConstructorCreation.actual;

import java.util.List;

public class Test {
    private Integer a;
    private List<String> b;
    private Integer c;
    private int d;
    private Integer e;

    public Test(Integer a, final List<String> b, int d) {
        this.a = a;
        this.b = b;
        this.d = d;
        c += 5;
    }

    public void fun1() {
        a += 1;
        b.add("1");
        c += 1;
    }

    public void fun2() {
        d += 1;
        e += 1;
    }
}