package TestFinalInitialisationWithConstructorParameters.actual;

public class Test {
    private int a;
    private int b;
    private int c;
    private final int d;
    private int e;

    public Test(int init) {
        a = 5;
        b = 5;
        c = 5;
        d = init;
        e = 5;
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