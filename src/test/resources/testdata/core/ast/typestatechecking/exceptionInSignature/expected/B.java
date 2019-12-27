import java.io.IOException;
import java.io.IOException;
import java.io.IOException;

/**
 * @see Main#B **/
public class B extends State {
    public int getState() {
        return Main.B;
    }

    public void main(Main main) throws IOException {
        System.out.println("B");
    }

    public void main2(Main main) throws IOException {
        main.throwException();
    }
}