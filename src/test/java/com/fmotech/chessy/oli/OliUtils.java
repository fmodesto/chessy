package com.fmotech.chessy.oli;

import java.util.Arrays;
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

    public static String think(String fen, int seconds, int depth) {
        Arrays.fill(OliThink.hashDB, 0);
        Arrays.fill(OliThink.hashDP, 0);
        Arrays.fill(OliThink.history, 0);
        Arrays.fill(OliThink.killer, 0);
        Arrays.fill(OliThink.hstack, 0);
        Arrays.fill(OliThink.mstack, 0);
        for (int i = 0; i < 64; i++) {
            Arrays.fill(OliThink.pv[i], 0);
        }

        OliThink._parse_fen(fen);
        OliThink.st = seconds;
        OliThink.calc(depth, 0);
        return OliThink.strmove(OliThink.pv[0][0]);
    }

    public static int evaluate(String fen) {
        OliThink._parse_fen(fen);
        return OliThink.eval(OliThink.onmove);
    }

    public static int see(String fen, String move) {
        OliThink._parse_fen(fen);
        int m = OliThink.parseMove(move, OliThink.onmove, 0);
        return OliThink.swap(m);
    }

    public static void load(String fen) {
        OliThink._parse_fen(fen);
    }
}
