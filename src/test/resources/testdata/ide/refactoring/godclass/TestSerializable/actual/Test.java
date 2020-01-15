package TestSerializable.actual;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Test implements Serializable {
    private TestProduct testProduct = new TestProduct();
    private static final long serialVersionUID = 1L;
    private int a;
    private int b;
    private transient int c;

    public void fun1() {
        a += 1;
        b += 1;
        c += 1;
    }

    public void fun2() {
        testProduct.fun2();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        testProduct.setD(stream.read());
        a = stream.read();
        stream.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(testProduct.getE());
        stream.writeObject(b);
        stream.defaultWriteObject();
    }
}