package TestSourceMemberAccessesInExtractedClass.actual;

public class Test {
    private int a;
    private int b;
    private int c;
    private int d;
    private Integer e;

    public void fun1() {
        a += 1;
        b += 1;
        c += 1;
        System.out.println(this.e);
        System.out.println((e * (e + 1) + e.getClass().getName().length()) * (this.e));
    }

    public void fun2() {
        d += 1;
        e += 1;
    }
}