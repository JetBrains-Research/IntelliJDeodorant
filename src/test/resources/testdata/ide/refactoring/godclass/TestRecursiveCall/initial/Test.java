package TestRecursiveCall.actual;

public class Test {
    private int a;
    private int b;
    private int c;
    private int d;
    private int e;

    public void fun1(Integer arg, Integer arg2) {
        a += arg;
        b += arg2;
        c += 1;
        fun1(arg, arg2);
    }

    public void fun2(Integer arg) {
        d += arg;
        e += 1;
        fun2(arg);
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