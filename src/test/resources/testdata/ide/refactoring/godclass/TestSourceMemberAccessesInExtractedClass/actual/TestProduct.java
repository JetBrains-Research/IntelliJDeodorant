package TestSourceMemberAccessesInExtractedClass.actual;

public class TestProduct {
    private int a;
    private int b;
    private int c;

    public void fun1(Integer thisE) {
        a += 1;
        b += 1;
        c += 1;
        System.out.println(thisE);
        System.out.println((thisE * (thisE + 1) + thisE.getClass().getName().length()) * (thisE));
    }
}