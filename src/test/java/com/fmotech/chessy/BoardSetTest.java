package com.fmotech.chessy;

import org.junit.Test;

import static org.junit.Assert.*;

public class BoardSetTest {

    @Test
    public void test() {
        BoardSet set = new BoardSet(16);
        assertTrue(set.add(32));
        assertTrue(set.add(37));
        assertFalse(set.add(32));
        set.remove(37);
        set.remove(32);
        assertTrue(set.add(32));
        assertTrue(set.add(37));
    }

    @Test(expected = IllegalStateException.class)
    public void testError() {
        BoardSet set = new BoardSet(16);
        assertTrue(set.add(32));
        assertTrue(set.add(48));
        assertFalse(set.add(32));
        set.remove(32);
    }
}