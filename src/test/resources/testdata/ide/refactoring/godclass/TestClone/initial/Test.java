package TestClone.actual;

public class Test implements Cloneable {
    private int a;
    private int b;
    private int c;
    private int d;
    private int e;

    public void fun1() {
        a += 1;
        b += 1;
        c += 1;
    }

    public void fun2() {
        d += 1;
        e += 1;
    }

    public Test clone() {
        Test test = new Test();
        test.a = a;
        test.b = b;
        test.c = c + 5;

        //Original plugin gives here `test.testProduct.setD(d);` This is bug. I have no motivation to find it and reproduce.
        test.d = d;

        test.e = e + d + a + b + c;
        return test;
    }
}