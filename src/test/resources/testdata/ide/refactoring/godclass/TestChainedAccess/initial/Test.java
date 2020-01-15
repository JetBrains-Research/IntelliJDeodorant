package TestChainedAccess.actual;

public class Test {
    private int a;
    private int b;
    private int c;
    private int d;
    private int e;
    private Test chained;
    private Test chained2;

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

        int[] arr = new int[this.chained.chained2.d];

        test.d = chained.chained2.chained.d + this.chained.chained2.chained.d;
        System.out.println(chained.d);
        System.out.println(chained.chained2.d);
        System.out.println(this.chained.d);
        System.out.println(this.chained.chained2.d);
        System.out.println(this.chained.chained2.d + chained.d);
        System.out.println(-this.chained.chained2.d);
        test.e = this.chained.chained2.d - test.chained.d + 5;

        test.e = arr[chained.chained2.a];
        test.e = e + d + a + b + c;

        this.chained.a = e;
        this.chained.chained2.chained.e = e;
        chained2.e = e;
        chained.chained2.chained.d = e;

        return test;
    }
}