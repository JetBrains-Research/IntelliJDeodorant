public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;

    public void main(int parameter) {
        int state = parameter;
        getStateObject(state).main();
    }

    private State getStateObject(int state) {
        switch (state) {
            case C:
                return new C();
            case B:
                return new B();
            case A:
                return new A();
        }
        return null;
    }
}