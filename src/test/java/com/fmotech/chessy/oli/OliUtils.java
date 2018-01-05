package com.fmotech.chessy.oli;

import java.util.Map;

public class OliUtils {

    static {
        OliThink.initialize();
    }

    public static long perft(String fen, int depth) {
        OliThink._parse_fen(fen);
        return OliThink.perft(OliThink.onmove, depth, 0);
    }

    public static Map<String, Long> divide(String fen, int depth) {
        OliThink._parse_fen(fen);
        OliThink.perft(OliThink.onmove, depth, 1);
        return OliThink.moves;
    }

    public static String think(String fen, int seconds) {
        OliThink._parse_fen(fen);
        OliThink.st = seconds;
        OliThink.calc(64, 0);
        return OliThink.strmove(OliThink.pv[0][0]);
    }

    public static int evaluate(String fen) {
        OliThink._parse_fen(fen);
        return OliThink.eval(OliThink.onmove);
    }
}
