package ru.tests;

/**
 * @see Main#A **/
public class A extends State {
    public int getState() {
        return Main.A;
    }

    public void main2() {
        System.out.println("main2 other");
        System.out.println("main2 other 2");
    }

    public void main1() {
        System.out.println("main1 A");
    }
}