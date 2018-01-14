package com.fmotech.chessy;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class HashTableTest {

    @Test
    public void test() {
        HashTable table = new HashTable(1 << 20);

        Random random = new Random(0);
        List<Long> hashes = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            hashes.add(random.nextLong());
            table.put(hashes.get(i), i);
        }
        for (int i = 0; i < 1000; i++) {
            assertEquals(i, table.get(hashes.get(i)));
        }
    }

}