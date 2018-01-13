package com.fmotech.chessy;

import com.fmotech.chessy.oli.OliUtils;
import org.junit.Test;

public class EngineTest {

    @Test
    public void test() {
        String fen = "7K/8/6Q1/8/8/8/k7/q7 w - - 5 4";
        Engine engine = new Engine(Board.load(fen));
        System.out.println(engine.calc(2, 64));
        String move = OliUtils.think(fen, 2, 64);
        System.out.println(move);

        System.out.println(OliUtils.evaluate(fen));
    }
}