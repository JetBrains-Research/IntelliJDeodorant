public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;

    State state;

    public int main() {
        int a = 1;
        a = state.main(a);
        return a;
    }

    public void setState(int state) {
        switch (state) {
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