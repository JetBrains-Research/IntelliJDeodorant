package TestSerializableTransientExtractedField.actual;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Test implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer a;
    private Integer b;
    private transient Integer c;
    private transient Integer d;
    private Integer e;

    public void fun1() {
        a += 1;
        b += 1;
        c += 1;
    }

    public void fun2() {
        d += 1;
        e += 1;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        d = stream.read();
        a = stream.read();
        stream.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(e);
        stream.writeObject(b);
        stream.defaultWriteObject();
    }
}