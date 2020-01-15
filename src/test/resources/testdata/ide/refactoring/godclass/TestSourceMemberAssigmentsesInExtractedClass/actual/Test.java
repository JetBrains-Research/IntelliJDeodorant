package TestSourceMemberAssigmentsesInExtractedClass.actual;

public class Test {
    private TestProduct testProduct = new TestProduct();
    private int d;
    private Integer e;

    public void fun1() {
        testProduct.fun1(this, this.e);
    }

    public void fun2() {
        d += 1;
        e += 1;
    }

    public void setD(int d) {
        this.d = d;
    }

    public int getD() {
        return d;
    }

    public void setE(Integer e) {
        this.e = e;
    }

    public Integer getE() {
        return e;
    }
}