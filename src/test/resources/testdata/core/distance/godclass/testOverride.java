package ru.hse.godclass;

public class TestSeparateBlocks extends AnotherClass {
    private int a;
    private int b;
    private int c;
    private int d;
    private int e;

    public String MethodToExtend() {
        a += 1;
        b += 1;
        c += 1;
        return "";
    }

    public synchronized void fun2() {
        d += 1;
        e += 1;
    }
}

class AnotherClass {
    public String MethodToExtend() {
        return "";
    }
}