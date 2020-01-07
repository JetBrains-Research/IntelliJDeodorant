package TestStaticFieldAccess2.actual;

public class Test {
    private int a = 1;
    private static Integer b = 2;
    private  int c = 3;
    private int d = 4;
    private int e = 5;

    public Test(int val) {
        b = val;
        a += b;
        b = a;
    }

    public void fun1() {
        a += b;
        b = (a + b + c) * (a + b);
        c = 1;
        c = (b + b + b);

        System.out.print(b);
        fun2(b);
    }

    public void fun2(int t) {
        d += t;
        e += t;
    }
}