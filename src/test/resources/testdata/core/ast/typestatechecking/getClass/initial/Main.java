public class Main {
    Base state;

    void main() {
        if (state.getClass() == A.class) {
            System.out.println("A");
        } else {
            System.out.println("other");
        }
    }
}