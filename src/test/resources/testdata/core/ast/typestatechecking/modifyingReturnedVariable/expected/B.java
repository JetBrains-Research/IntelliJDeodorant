package ru.tests;

/**
 * @see Main#B **/
public class B extends State {
    public int getState() {
        return Main.B;
    }

    public int main(int a) {
        System.out.println("B");
        return a;
    }
}