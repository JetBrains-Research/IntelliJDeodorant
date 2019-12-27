public class Main {
    public void main(StateEnum state) {
        getStateObject(state).main();
    }

    private State getStateObject(StateEnum state) {
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

    private enum StateEnum {
        A, B, C
    }
}