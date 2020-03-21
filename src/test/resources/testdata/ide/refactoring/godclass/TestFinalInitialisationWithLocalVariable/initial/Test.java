package TestFinalInitialisationWithLocalVariable.actual;

public class Test {
    private int a;
    private int b;
    private int c;
    private final int d;
    private final int e;

    public Test(int init) {
        int local = init;

        a = 5;
        b = 5;
        c = 5;
        d = local;
        e = local;
    }

    public void fun1() {
        System.out.print(a);
        System.out.print(b);
        System.out.print(c);
    }

    public void fun2() {
        System.out.print(d);
        System.out.print(e);
    }

    public void fun3() {
        System.out.print(a);
        System.out.print(b);
        System.out.print(c);
        System.out.print(d);
        System.out.print(e);
    }
}