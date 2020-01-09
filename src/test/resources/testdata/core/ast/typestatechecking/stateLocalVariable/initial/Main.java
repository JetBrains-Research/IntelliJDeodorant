public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;

    public void main(int parameter) {
        int state = parameter;
        if (state == A) {
            System.out.println("A");
        } else if (state == B) {
            System.out.println("B");
        } else if (state == C) {
            System.out.println("C");
        }
    }
}