public class Main {
    Base state;

    void main() {
        if (state instanceof A) {
            System.out.println("A");
        } else {
            System.out.println("other");
        }
    }
}