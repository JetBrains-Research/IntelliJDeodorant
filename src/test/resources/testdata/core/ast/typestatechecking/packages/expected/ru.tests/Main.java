package ru.tests;

public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;
    State state;

    public void main1() {
        state.main1();
    }

    public void main2() {
        state.main2();
    }

    public void setState(int state) {
        switch (state) {
            case C:
                this.state = new C();
                break;
            case B:
                this.state = new B();
                break;
            case A:
                this.state = new A();
                break;
            default:
                this.state = null;
                break;
        }
    }

    public int getState() {
        return state.getState();
    }
}