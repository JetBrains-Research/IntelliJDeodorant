package ru.hse.godclass;

public class testSeparateBlocks {
    private int a;
    private int b;
    private int c;
    private int d;
    private int e;

    public synchronized void fun1() {
        a += 1;
        b += 1;
        c += 1;
    }

    public synchronized void fun2() {
        d += 1;
        e += 1;
    }
}