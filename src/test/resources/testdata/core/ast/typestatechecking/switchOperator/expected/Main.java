public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;
    public final static int D = 4;
    State state;

    public void main() {
        state.main();
    }

    public void setC() {
        setState(C);
    }

    public void setD() {
        setState(D);
    }

    public void setState(int state) {
        switch (state) {
            case A:
                this.state = new A();
                break;
            case C:
                this.state = new C();
                break;
            case D:
                this.state = new D();
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