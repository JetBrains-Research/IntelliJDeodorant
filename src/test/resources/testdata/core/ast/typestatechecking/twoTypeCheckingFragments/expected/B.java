/**
 * @see Main#B **/
public class B extends State {
    public int getState() {
        return Main.B;
    }

    public void main2() {
        System.out.println("main2 B");
    }

    public void main1() {
        System.out.println("main1 other");
    }
}