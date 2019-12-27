public class Main {
    public final static int A = 1;
    public final static int B = 2;

    public void main(int state) {
        getStateObject(state).main();
    }

    private State getStateObject(int state) {
        switch (state) {
            case B:
                return new B();
            case A:
                return new A();
        }
        return null;
    }
}