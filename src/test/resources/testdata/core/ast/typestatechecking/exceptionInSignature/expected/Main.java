import java.io.IOException;

public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;
    State state;

    public void main() throws IOException {
        state.main(this);
    }

    public void main2() throws IOException {
        state.main2(this);
    }

    public void throwException() throws IOException {
        throw new IOException();
    }

    public void setState(int state) {
        switch (state) {
            case B:
                this.state = new B();
                break;
            case A:
                this.state = new A();
                break;
            case C:
                this.state = new C();
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