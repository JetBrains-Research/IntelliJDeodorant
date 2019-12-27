import java.io.IOException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.Set;

public abstract class State {
    public abstract int getState();

    public abstract void main(ArrayList<Set<Integer>> list);

    public abstract void main2(OutputStream output) throws IOException;
}