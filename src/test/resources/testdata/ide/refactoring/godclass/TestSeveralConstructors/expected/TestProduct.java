package TestSeveralConstructors.actual;


import java.util.List;

public class TestProduct {
    private Integer a;
    private List<String> b;
    private Integer c = 6;

    public void setA(Integer a) {
        this.a = a;
    }

    public void setB(List<String> b) {
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