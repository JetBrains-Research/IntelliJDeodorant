public class TestClass {
    boolean testAnnotation = false;
    boolean lol = false;

    void test() {
    }

    public class TEST {
        boolean d;

        public class NESTEDTEST {
            void fun() {
                boolean a = TestClass.this.testAnnotation;
                boolean b = TestClass.this.lol;
                boolean c = false;
                boolean e = d;
            }
        }
    }
}