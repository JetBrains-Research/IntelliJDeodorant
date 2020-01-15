package TestCircleDependency.actual;

public class TestProduct {
    private int a;
    private int b;
    private int c;

    public void fun1(Test test) {
        fun3(test);
        a += 1;
        b += 1;
        c += 1;
    }

    public void fun3(Test test) {
        a = a + 6;
        b *= 8;
        c = a - b + c;
        test.fun4();
    }
}