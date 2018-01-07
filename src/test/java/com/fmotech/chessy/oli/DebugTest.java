package com.fmotech.chessy.oli;

import com.fmotech.chessy.Board;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class DebugTest {

    @Test
    public void evaluate() throws Exception {
        Files.lines(Paths.get("/Users/fran/IdeaProjects/chess/boards"))
                .limit(10000000)
                .forEach(this::testEvaluation);
    }

    private void testEvaluation(String fen) {
        Board board = Board.load(fen);
        OliUtils.load(fen);
//        System.out.println(fen);
        assertEquals(fen, OliThink.HASHP(0), board.hash(0));
    }
}
