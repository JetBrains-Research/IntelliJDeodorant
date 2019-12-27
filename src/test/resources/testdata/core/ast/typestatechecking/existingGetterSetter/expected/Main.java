public class Main {
    public final static int A = 1;
    public final static int B = 2;
    State state;

    public void main() {
        state.main();
    }

    public void setState(int state) {
        switch (state) {
            case A:
                this.state = new A();
                break;
            case B:
                this.state = new B();
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