package ru.hse.godclass;

public class testSeparateBlocksWithStrictOrder {
    private int aa;
    private int ab;
    private int ac;
    private int ad;
    private int ae;
    private int af;
    private int ag;


    private int ba;
    private int bb;
    private int bc;
    private int bd;
    private int be;
    private int bf;

    private int ca;
    private int cb;
    private int cc;
    private int cd;
    private int ce;

    private int da;
    private int db;
    private int dc;
    private int dd;

    private int ea;
    private int eb;
    private int ec;

    private int fa;
    private int fb;

    public void fun1() {
        aa++;
        ab++;
        ac++;
        ad++;
        ae++;
        af++;
        ag++;
    }

    public void fun2() {
        ba++;
        bb++;
        bc++;
        bd++;
        be++;
        bf++;
    }

    public void fun3() {
        ca++;
        cb++;
        cc++;
        cd++;
        ce++;
    }

    public void fun4() {
        da++;
        db++;
        dc++;
        dd++;
    }

    public void fun5() {
        ea++;
        eb++;
        ec++;
    }

    public void fun6() {
        fa++;
        fb++;
    }
}