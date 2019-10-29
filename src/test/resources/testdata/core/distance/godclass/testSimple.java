package ru.hse.godclass;

public class Simple {
    public int toto = 10;

    private int SIZE = 100;
    private int[] array = new int[SIZE];
    private int random = 5;

    public int getSumMult() {
        changeA();
        int sum = 0;
        for (int i = 0; i < SIZE; i++) {
            sum = sum * array[i] + array[i];
        }
        sum += 10;
        return sum;
    }

    public int getSumMult2() {
        changeA();
        int sum = 0;
        SIZE += 5;
        for (int i = 0, j = 0; i < SIZE; i++, j++) {
            sum = sum * array[i] + array[i];
        }
        sum += toto;
        return sum;
    }

    public void changeA() {
        array[5] += random;
    }
}