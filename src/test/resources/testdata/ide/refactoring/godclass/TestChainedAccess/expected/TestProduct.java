package TestChainedAccess.actual;


public class TestProduct {
    private int d;
    private int e;

    public int getD() {
        return d;
    }

    public void setD(int d) {
        this.d = d;
    }

    public int getE() {
        return e;
    }

    public void setE(int e) {
        this.e = e;
    }

    public void fun2() {
        d += 1;
        e += 1;
    }
}