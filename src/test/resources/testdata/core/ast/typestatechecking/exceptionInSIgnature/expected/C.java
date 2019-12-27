import java.io.IOException;
import java.io.IOException;
import java.io.IOException;

/**
 * @see Main#C **/
public class C extends State {
    public int getState() {
        return Main.C;
    }

    public void main(Main main) throws IOException {
    }

    public void main2(Main main) throws IOException {
        main.throwException();
    }
}