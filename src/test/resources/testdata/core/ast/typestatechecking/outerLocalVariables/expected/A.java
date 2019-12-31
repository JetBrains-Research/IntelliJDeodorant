import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 * @see Main#A **/
public class A extends State {
    public int getState() {
        return Main.A;
    }

    public void main(ArrayList<Set<Integer>> list) {
        list.add(Collections.emptySet());
    }

    public void main2(OutputStream output) throws IOException {
        System.out.println("other");
    }
}