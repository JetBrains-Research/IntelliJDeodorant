package SeveralConstructors.actual;

import java.util.List;

public class Test {
    private Integer a;
    private List<String> b;
    private Integer c = 6;
    private int d;
    private Integer e;

    public Test(Integer a, List<String> b, int d) {
        this.a = a;
        this.b = b;
        this.d = d;
        e = 6;
    }

    public Test(Integer a, List<String> b) {
        this.a = a;
        this.b = b;
    }

    public Test(int field) {
        a = field;
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