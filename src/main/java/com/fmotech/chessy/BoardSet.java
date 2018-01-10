package com.fmotech.chessy;

public class BoardSet {

    private final long[] table;
    private final long mask;
    private final long invert;
    private int next = 1;

    public BoardSet(int size) {
        size = nextPowerOfTwo(size);
        table = new long[size];
        mask = (size >>> 1) - 1;
        invert = ~mask;
    }

    public static int nextPowerOfTwo(int x) {
        if (x == 0) return 1;
        x--;
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        x |= x >> 16;
        return x + 1;
    }

    public boolean add(long hash) {
        int entry = (int) (hash & mask);
        while (table[entry] != 0) {
            if (((table[entry] ^ hash) & invert) == 0)
                return false;
            if ((table[entry] & mask) == 0)
                table[entry] = table[entry] | next++;
            entry = (int) (table.length - (table[entry] & mask));
        }
        table[entry] = hash & invert;
        return true;
    }

    public boolean contains(long hash) {
        int entry = (int) (hash & mask);
        while (table[entry] != 0) {
            if (((table[entry] ^ hash) & invert) == 0)
                return true;
            if ((table[entry] & mask) == 0)
                return false;
            entry = (int) (table.length - (table[entry] & mask));
        }
        return false;
    }

    public void remove(long hash) {
        int prev = -1;
        int entry = (int) (hash & mask);
        while (((table[entry] ^ hash) & invert) != 0) {
            prev = entry;
            entry = (int) (table.length - (table[entry] & mask));
        }
        if ((table[entry] & mask) != 0)
            throw new IllegalStateException("Removing non last entry");
        if (prev >= 0) {
            table[prev] &= invert;
            next -= 1;
        }
        table[entry] = 0;
    }
}
