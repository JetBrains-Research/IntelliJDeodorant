import java.io.*;
import java.util.ArrayList;
import java.util.Set;

public class Main {
    public final static int A = 1;
    public final static int B = 2;
    public final static int C = 3;
    public int state;

    public void main() {
        if (state == A) {
            System.out.println("A");
            ArrayList<Set<Integer>> list = new ArrayList<>();
            InputStream input = new ByteArrayInputStream(new byte[0]);
        } else if (state == B) {
            System.out.println("B");
        }
    }

    public void main2()  {
        if (state == C) {
            OutputStream output = new ByteArrayOutputStream();
        } else {
            System.out.println("other");
        }
    }
}