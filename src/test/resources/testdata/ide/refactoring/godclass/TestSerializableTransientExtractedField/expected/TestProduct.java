package TestSerializableTransientExtractedField.actual;


import java.io.Serializable;

public class TestProduct implements Serializable {
    private transient Integer d;
    private Integer e;

    public void setD(Integer d) {
        this.d = d;
    }

    public Integer getE() {
        return e;
    }

    public void fun2() {
        d += 1;
        e += 1;
    }
}