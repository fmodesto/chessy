package com.fmotech.chessy;

import com.fmotech.chessy.oli.OliThink;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MoveTablesTest {

    @BeforeClass
    public static void initialize() {
        OliThink.initialize();
    }

    @Test
    public void testRays() {
        assertArrayEquals(OliThink.rays, MoveTables.RAYS);
    }

    @Test
    public void testPawn() {
        for (int i = 0; i < 64; i++) {
            assertEquals(OliThink.pcaps[0][i], MoveTables.PAWN_ATTACK[0][i]);
            assertEquals(OliThink.pcaps[1][i], MoveTables.PAWN_ATTACK[1][i]);
        }
    }

    @Test
    public void testKnight() {
        assertArrayEquals(OliThink.nmoves, MoveTables.KNIGHT);
    }

    @Test
    public void testKing() {
        assertArrayEquals(OliThink.kmoves, MoveTables.KING);
    }

}