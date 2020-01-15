package TestStaticFieldAccess2.actual;

public class TestProduct {
    private int a = 1;
    private int c = 3;

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    public void fun1(Test test) {
        a += Test.b;
        Test.b = (a + Test.b + c) * (a + Test.b);
        c = 1;
        c = (Test.b + Test.b + Test.b);

        System.out.print(Test.b);
        test.fun2(Test.b);
    }
}