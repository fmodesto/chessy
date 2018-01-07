package com.fmotech.chessy;

import com.fmotech.chessy.oli.OliUtils;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class EvaluationTest {

    private Evaluation evaluation = new Evaluation();
    static int counter = 0;
    static int failure = 0;

    @Test
    public void evaluate() throws Exception {
        Files.lines(Paths.get("/Users/fran/IdeaProjects/chess/boards"))
                .limit(100000)
                .forEach(this::testEvaluation);
        System.out.println(failure + " " + counter);
        // 0 110474295
    }

    static  int score = 0;

    private void testEvaluation(String fen) {
        Board board = Board.load(fen);
        counter += 1;
        int expected = OliUtils.evaluate(fen);
        int actual = evaluation.evaluate(board, board.sideToMove());
        if (expected != actual) {
            failure += 1;
            int s = Math.abs(expected - actual);
            if (s > score) {
                score = s;
                System.out.println((failure / (double) counter) + " " + s + " " + fen);
            }
        }
    }

    @Test
    public void debug() {
        String fen = "4r3/1r1b1k1p/3Pp3/p1Pp2q1/3B2Q1/7P/1P3P2/4R1K1 w - - 0 45";
        Board board = Board.load(fen);
        board.print();
        System.out.println(fen);
        System.out.println(OliUtils.evaluate(fen));
        System.out.println(evaluation.evaluate(board, board.sideToMove()));
    }
}