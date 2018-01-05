package com.fmotech.chessy;

import com.fmotech.chessy.oli.OliUtils;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class EvaluationTest {

    private Evaluation evaluation = new Evaluation();

    @Test
    public void evaluate() throws Exception {
        Files.lines(Paths.get("/Users/fran/IdeaProjects/chess/boards"))
                .forEach(this::testEvaluation);
    }

    private void testEvaluation(String fen) {
        Board board = Board.load(fen);
        assertEquals(fen, evaluation.evaluate(board, board.sideToMove()), OliUtils.evaluate(fen));
    }

    @Test
    public void debug() {
        String fen = "r1bqkbnr/pp1ppp1p/2B3p1/2p5/4P3/2N5/PPPP1PPP/R1BQK1NR b KQkq - 0 4 ";
        Board board = Board.load(fen);
        evaluation.evaluate(board, board.sideToMove());
        OliUtils.evaluate(fen);
    }
}