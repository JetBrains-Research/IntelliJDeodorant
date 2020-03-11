package TestSimple.actual;

public class Test {
    private TestProduct testProduct = new TestProduct();

    private int random = 5;

    public int getSumMult() {
        return testProduct.getSumMult(this);
    }

    public int getSumMult2() {
        return testProduct.getSumMult2(this);
    }

    public void changeA() {
        testProduct.getArray()[5] += random;
    }
}