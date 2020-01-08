import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;

/**
 * @see Main#C **/
public class C extends State {
    public int getState() {
        return Main.C;
    }

    public void main(ArrayList<Set<Integer>> list) {
    }

    public void main2(OutputStream output) throws IOException {
        output.write(1);
    }
}