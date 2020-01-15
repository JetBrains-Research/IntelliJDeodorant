package TestConstructorCreation.actual;


import java.util.List;

public class TestProduct {
    private Integer a;
    private List<String> b;
    private Integer c;

    public void setA(Integer a) {
        this.a = a;
    }

    public void setB(List<String> b) {
        this.b = b;
    }

    public Integer getC() {
        return c;
    }

    public void setC(Integer c) {
        this.c = c;
    }

    public void fun1() {
        a += 1;
        b.add("1");
        c += 1;
    }
}