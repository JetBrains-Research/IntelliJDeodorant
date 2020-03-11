package ru.tests;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;

/**
 * @see Main#A **/
public class A extends State {
    public int getState() {
        return Main.A;
    }

    public void main() {
        System.out.println("A");
        ArrayList<Set<Integer>> list = new ArrayList<>();
        InputStream input = new ByteArrayInputStream(new byte[0]);
    }

    public void main2() {
        System.out.println("other");
    }
}