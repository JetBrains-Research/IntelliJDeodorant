package TestRecursiveCall.actual;


public class TestProduct {
    private int a;
    private int b;
    private int c;

    public void fun1(Integer arg, Integer arg2) {
        a += arg;
        b += arg2;
        c += 1;
        fun1(arg, arg2);
    }

    public void fun3(Test test) {
        a = a + 6;
        b *= 8;
        c = a - b + c;
        test.fun4();
    }
}