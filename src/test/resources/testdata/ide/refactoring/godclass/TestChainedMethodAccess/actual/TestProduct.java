package TestChainedMethodAccess.actual;

public class TestProduct {
    private Test chained;
    private int b;
    private int c;

    public Test getChained() {
        return chained;
    }

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }

    public Test fun1() {
        b += 1;
        c += 1;
        return new Test();
    }

    public Test fun4() {
        chained.b += 1;
        b += 1;
        c += 1;
        return new Test();
    }
}