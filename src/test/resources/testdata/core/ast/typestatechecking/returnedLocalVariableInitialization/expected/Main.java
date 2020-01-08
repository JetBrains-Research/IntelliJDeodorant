public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;

    State state;

    public int main() {
        int a = 0;
        if (getState() != A) {
            setState(B);
        }
        a = state.main(a);
        return a;
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