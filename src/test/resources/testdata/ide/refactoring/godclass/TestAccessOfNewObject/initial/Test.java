package TestAccessOfNewObject.actual;

public class Test {
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

    public Test fun3() {
        Test test = new Test();
        System.out.println(test.d);
        test.a = a;
        test.b = b;
        test.c = c + 5;

        //This is a bug in the original plugin. Actually it gives here test.testProduct.setD(d);
        //I have no motivation to find and reproduce this bug.
        test.d = d;

        test.e = e + d + a + b + c;
        return test;
    }
}