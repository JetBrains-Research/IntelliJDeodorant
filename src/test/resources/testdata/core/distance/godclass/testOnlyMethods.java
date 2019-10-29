package ru.hse.godclass;

public class testOnlyMethods {
    public void fun1() {
        fun2();
        fun3();
    }

    public void fun2() {
        fun1();
        fun3();
    }

    public void fun3() {
        fun1();
        fun2();
    }

    public void fun4() {
        fun5();
        fun6();
    }

    public void fun5() {
        fun4();
    }

    public void fun6() {
        fun5();
        fun4();
    }
}