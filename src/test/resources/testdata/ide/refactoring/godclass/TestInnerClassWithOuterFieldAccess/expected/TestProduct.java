package TestInnerClassWithOuterFieldAccess.actual;


public class TestProduct {
    private int a;
    private int b;
    private int c;

    public void fun1() {
        a += 1;
        b += 1;
        c += 1;
        OuterTest.outer += c + OuterTest.outer;
    }
}