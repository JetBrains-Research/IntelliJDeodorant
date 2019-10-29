public class TestClass {
    boolean testAnnotation = false;
    boolean lol = false;

    void test() {
    }

    public class TOST {
        boolean d;

        void fun() {
            boolean a = TestClass.this.testAnnotation;
            boolean b = TestClass.this.lol;
            boolean c = false;
            boolean e = d;
        }
    }
}