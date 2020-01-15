package TestSourceMemberAssigmentsesInExtractedClass.actual;

public class TestProduct {
    private int a;
    private int b;
    private int c;

    public void fun1(Test test, Integer thisE) {
        a += 1;
        b += 1;
        c += 1;
        test.setD(thisE);
        test.setD(test.getD() - thisE);
        test.setD((thisE * (thisE + 1) + thisE.getClass().getName().length()) * (thisE));
        test.setE(test.getE() + test.getD());
        test.setD(test.getD() + test.getD());
        test.setD(test.getD() + test.getD() + test.getD());
        test.setD(test.getD() + 1);
    }
}