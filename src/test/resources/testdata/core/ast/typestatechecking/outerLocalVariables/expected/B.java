import java.io.IOException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.Set;

/**
 * @see Main#B **/
public class B extends State {
    public int getState() {
        return Main.B;
    }

    public void main(ArrayList<Set<Integer>> list) {
        System.out.println("B");
    }

    public void main2(OutputStream output) throws IOException {
        System.out.println("other");
    }
}