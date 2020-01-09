public class Main {
    public final static int A = 1;
    public final static int B = 2;
    int state;

    public void main() {
        switch (state) {
            case A:
                System.out.println("A");
            case B:
                System.out.println("Other");
        }
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }
}