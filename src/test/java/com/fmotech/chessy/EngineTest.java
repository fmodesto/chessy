package com.fmotech.chessy;

import com.fmotech.chessy.oli.OliUtils;
import org.junit.Test;

public class EngineTest {

    @Test
    public void test() {
        String fen = "8/7P/8/6k1/6p1/8/7K/r3q3 w - - 0 63";
        Engine engine = new Engine(Board.load(fen));
        System.out.println(engine.think(2000, 64));
        String move = OliUtils.think(fen, 2000, 64);
        System.out.println(move);

        System.out.println(OliUtils.evaluate(fen));
    }
}