package ru.hse.godclass;

public class testSeparateBlocks {
    public int a;
    public int b;
    public int c;
    public int d;
    public int e;

    public void fun1() {
        a += 1;
        b += 1;
        c += 1;
    }

    public void fun2() {
        d += 1;
        e += 1;
    }
}

class someClass {
    testSeparateBlocks test = new testSeparateBlocks();

    public void fun() {
        System.out.print(test.a);
        System.out.print(test.d);
    }
}