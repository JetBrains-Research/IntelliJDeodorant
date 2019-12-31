public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;

    public int state;

    public int main() {
        int a;
        if (state != A) {
            state = B;
        }
        if (state == A) {
            a = 3;
        } else {
            a = 2;
        }
        return a;
    }
}