package TestSerializable.actual;


import java.io.Serializable;

public class TestProduct implements Serializable {
    private int d;
    private int e;

    public void setD(int d) {
        this.d = d;
    }

    public int getE() {
        return e;
    }

    public void fun2() {
        d += 1;
        e += 1;
    }
}