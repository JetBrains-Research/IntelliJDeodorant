public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;
    public final static int D = 4;
    int state;

    public void main() {
        if (state == A) {
            System.out.println("A");
        } else if (state == B) {
            System.out.println("B");
        } else {
            System.out.println("C");
        }
    }

    public void setC() {
        state = C;
    }

    public void setD() {
        state = D;
    }
}