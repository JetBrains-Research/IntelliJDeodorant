package ru.tests;

/**
 * @see Main#C **/
public class C extends State {
    public int getState() {
        return Main.C;
    }

    public void main2() {
        System.out.println("main2 C");
    }

    public void main1() {
        System.out.println("main1 other");
    }
}