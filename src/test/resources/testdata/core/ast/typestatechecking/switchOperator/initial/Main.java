public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;
    public final static int D = 4;
    int state;

    public void main() {
        switch (state) {
            case A:
                System.out.println("A");
                break;
            default:
                System.out.println("Other");
        }
    }

    public void setC() {
        state = C;
    }

    public void setD() {
        state = D;
    }
}