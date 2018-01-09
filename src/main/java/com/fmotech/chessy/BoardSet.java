package com.fmotech.chessy;

public class BoardSet {

    private final long[] table;
    private final long mask;
    private final long invert;
    private int next = 1;

    public BoardSet(int size) {
        if ((BitOperations.southFill(size) + 1 >>> 1) != size)
            size = (int) (BitOperations.southFill(size) + 1);
        table = new long[size];
        mask = (size >>> 1) - 1;
        invert = ~mask;
    }

    public boolean add(long hash) {
        int entry = (int) (hash & mask);
        while (table[entry] != 0) {
            if (((table[entry] ^ hash) & invert) == 0)
                return false;
            if ((table[entry] & mask) == 0)
                table[entry] |= next++;
            entry = (int) (table.length - (table[entry] & mask));
        }
        table[entry] = hash & invert;
        return true;
    }

    public void remove(long hash) {
        int entry = (int) (hash & mask);
        while (((table[entry] ^ hash) & invert) != 0) {
            entry = (int) (table.length - (table[entry] & mask));
        }
        if ((table[entry] & mask) != 0)
            throw new IllegalStateException("Removing non last entry");
        if (entry > mask)
            next -= 1;
        table[entry] = 0;
    }
}
