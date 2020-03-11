package TestCircleDependency.actual;

public class Test {
    private int a;
    private int b;
    private int c;
    private int d;
    private int e;

    public void fun1() {
        fun3();
        a += 1;
        b += 1;
        c += 1;
    }

    public void fun2() {
        d += 1;
        e += 1;
        fun3();
    }

    private void fun3() {
        a = a + 6;
        b *= 8;
        c = a - b + c;
        fun4();
    }

    private void fun4() {
        e *= 2;
        fun3();
    }
}