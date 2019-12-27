import java.io.IOException;

public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;
    public int state;

    public void main() throws IOException {
        if (state == A) {
            System.out.println("A");
            throwException();
        } else if (state == B) {
            System.out.println("B");
        }
    }

    public void main2() throws IOException {
        if (state == C) {
            throwException();
        } else {
            throwException();
        }
    }

    public void throwException() throws IOException {
        throw new IOException();
    }
}