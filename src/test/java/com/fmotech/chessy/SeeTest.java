package com.fmotech.chessy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SeeTest {
    
    private See see = new See();

    @Test
    public void chessProgrammingExample1() {
        Board board = Board.load("1k1r4/1pp4p/p7/4p3/8/P5P1/1PP4P/2K1R3 w - - 0 1");
        int move = Formatter.moveFromSan(board, "Rxe5");
        assertEquals(100, see.evaluate(board, move));
    }

    @Test
    public void chessProgrammingExample2() {
        Board board = Board.load("1k1r3q/1ppn3p/p4b2/4p3/8/P2N2P1/1PP1R1BP/2K1Q3 w - - 0 1");
        int move = Formatter.moveFromSan(board, "Nxe5");
        assertEquals(-190, see.evaluate(board, move));
    }

    @Test
    public void enPassant() {
        Board board = Board.load("8/4K3/3p4/1Pp3kr/2r2p2/8/4P1P1/8 w - c6 0 3");
        int move = Formatter.moveFromFen(board, "b5c6");
        assertEquals(0, see.evaluate(board, move));
    }

    @Test
    public void dualKing() {
        Board board = Board.load("8/2p5/2Kp4/1P6/R4pk1/7r/4P1P1/8 w - - 3 3");
        int move = Formatter.moveFromFen(board, "g2h3");
        assertEquals(400, see.evaluate(board, move));
    }

    @Test
    public void kingFight() {
        Board board = Board.load("8/8/4K3/2kr4/8/8/3Q4/8 w KQkq - 0 1");
        int move = Formatter.moveFromFen(board, "d2d5");
        assertEquals(500, see.evaluate(board, move));
    }

    @Test
    public void promotion() {
        Board board = Board.load("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/1PN2Q1P/P1PBBPp1/R3K2R b KQkq - 0 2");
        int move = Formatter.moveFromFen(board, "g2h1q");
        assertEquals(400, see.evaluate(board, move));
    }
}
