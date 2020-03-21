package TestFinalInitialisationWithConstructorParameters.actual;


public class TestProduct {
    private final int d;
    private int e;

    public TestProduct(int init) {
        d = init;
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