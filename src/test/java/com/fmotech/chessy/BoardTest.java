package com.fmotech.chessy;

import org.junit.Test;

public class BoardTest {

    @Test
    public void load() {
        Board board = Board.load("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        board.print();
        System.out.println(DebugUtils.debug(board.hash()));
    }
}