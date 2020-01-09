import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;
    public int state;

    public void main() {
        ArrayList<Set<Integer>> list = new ArrayList<>();
        if (state == A) {
            list.add(Collections.emptySet());
        } else if (state == B) {
            System.out.println("B");
        }
    }

    public void main2() throws IOException {
        OutputStream output = new ByteArrayOutputStream();
        if (state == C) {
            output.write(1);
        } else {
            System.out.println("other");
        }
    }
}