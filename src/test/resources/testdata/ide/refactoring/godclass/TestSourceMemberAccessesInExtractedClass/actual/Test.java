package TestSourceMemberAccessesInExtractedClass.actual;

public class Test {
    private TestProduct testProduct = new TestProduct();
    private int d;
    private Integer e;

    public void fun1() {
        testProduct.fun1(this.e);
    }

    public void fun2() {
        d += 1;
        e += 1;
    }
}