package com.fmotech.chessy;

import com.fmotech.chessy.oli.OliUtils;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static com.fmotech.chessy.MoveGenerator.attackingPieces;
import static com.fmotech.chessy.MoveGenerator.generate;
import static com.fmotech.chessy.MoveGenerator.pinnedPieces;
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

    private int total = 0;

    @Test
    public void evaluate() throws Exception {
        Files.lines(Paths.get("/Users/fran/IdeaProjects/chess/boards"))
                .limit(1000000)
                .forEach(this::testEvaluation);
        System.out.println(total);
    }

    private void testEvaluation(String fen) {
        Board board = Board.load(fen);
        int[] captures = generateCaptures(board);
        for (int i = 1; i < captures[0]; i++) {
            total += see.evaluate(board, captures[i]);
//            total += captures[i];
        }
    }

    private int[] generateCaptures(Board board) {
        int sideToMove = board.sideToMove();
        long check = attackingPieces(board.kingPosition(sideToMove), sideToMove, board);
        if (check != 0) return new int[] { 1 };
        long pin = pinnedPieces(board.kingPosition(sideToMove), sideToMove, board);
        return generate(check, pin, sideToMove, 0, true, false, false, board);
    }

    @Test
    public void debug() {
        String fen = "4r2k/5qp1/3n3p/1p1P1pbP/2p1pP2/2P3PB/1P2Q1N1/3R2K1 b - f3 0 37";
        String move = "e4f3";
        Board board = Board.load(fen);
        int m = Formatter.moveFromFen(board, move);
        System.out.println(m);
        System.out.println(see.evaluate(board, m));
        System.out.println(OliUtils.see(fen, move));
    }

}
