package ru.tests;

public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;
    int state;

    public void main1() {
        if (state == A) {
            System.out.println("main1 A");
        } else {
            System.out.println("main1 other");
        }
    }

    public void main2() {
        if (state == B) {
            System.out.println("main2 B");
        } else if (state == C) {
            System.out.println("main2 C");
        } else {
            System.out.println("main2 other");
            System.out.println("main2 other 2");
        }
    }
}