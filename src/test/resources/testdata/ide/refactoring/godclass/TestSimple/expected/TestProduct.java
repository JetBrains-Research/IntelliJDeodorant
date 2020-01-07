package TestSimple.actual;

public class TestProduct {
    private int toto = 10;
    private int[] array = new int[6];
    private int SIZE = 100;

    public int[] getArray() {
        return array;
    }

    public int getSumMult(Test test) {
        test.changeA();
        int sum = 0;
        SIZE += 1;
        for (int i = 0; i < SIZE; i++) {
            sum = sum * array[i] + array[i];
        }
        sum += 10;
        return sum;
    }

    public int getSumMult2(Test test) {
        test.changeA();
        int sum = 0;
        SIZE += 5;
        for (int i = 0, j = 0; i < SIZE; i++, j++) {
            sum = sum * array[i] + array[i];
        }
        sum += toto;
        return sum;
    }
}