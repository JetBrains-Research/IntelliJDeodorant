public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;

    public int state;

    public int main() {
        int a = 1;
        if (state == A) {
            a = 3;
        } else if (state == B) {
            System.out.println("B");
        }
        return a;
    }
}