package com.fmotech.chessy;

public class BoardTable {

    private final long[] table;
    private final long mask;
    private final long invert;

    public BoardTable(int size) {
        this.table = new long[size];
        this.mask = size - 1;
        this.invert = ~mask;
    }

    public void put(long hash, int data) {
        table[(int) (hash & mask)] = hash & invert | data & mask;
    }

    public int get(long hash) {
        long data = table[(int) (hash & mask)];
        return ((data ^ hash) & invert) == 0 ? (int) (data & mask) : 0;
    }

    public boolean containsKey(long hash) {
        return ((table[(int) (hash & mask)] ^ hash) & invert) == 0;
    }
}
