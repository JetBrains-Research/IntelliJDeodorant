import java.io.IOException;
import java.io.IOException;
import java.io.IOException;

/**
 * @see Main#A **/
public class A extends State {
    public int getState() {
        return Main.A;
    }

    public void main(Main main) throws IOException {
        System.out.println("A");
        main.throwException();
    }

    public void main2(Main main) throws IOException {
        main.throwException();
    }
}