package TestStaticFieldAccess.actual;

public class TestProduct {
    private int d = 4;
    private int e = 5;

    public void fun2(int t) {
        d += t;
        e += t;
    }
}