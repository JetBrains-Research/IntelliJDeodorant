package TestFinalInitialisationWithLocalVariable.actual;


public class TestProduct {
    private final int d;
    private int e;

    public TestProduct(int local) {
        d = local;
    }

    public int getD() {
        return d;
    }

    public int getE() {
        return e;
    }

    public void setE(int e) {
        this.e = e;
    }

    public void fun2() {
        System.out.print(d);
        System.out.print(e);
    }
}