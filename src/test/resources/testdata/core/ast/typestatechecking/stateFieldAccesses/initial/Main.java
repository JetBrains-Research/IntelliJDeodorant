public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public int state;

    public void main() {
        if (state == A) {
            System.out.println("A");
        } else if (state == B) {
            System.out.println("B");
        }
    }

    public void function(int parameter) {
        // hello
    }

    public void useStateField() {
        int newState = state;
        function(state);
        state = 1;
        state = state & 1;
    }
}