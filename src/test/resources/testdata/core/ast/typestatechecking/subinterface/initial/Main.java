public class Main {

    public State state;

    public void main(int k) {
        if (state instanceof A) {
            System.out.println("A");
        } else if (state instanceof B) {
            System.out.println("B");
        }
    }
}