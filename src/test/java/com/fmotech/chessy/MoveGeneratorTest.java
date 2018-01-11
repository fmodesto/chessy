package com.fmotech.chessy;

import com.fmotech.chessy.oli.OliUtils;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.fmotech.chessy.MoveTables.DIR;
import static com.fmotech.chessy.MoveTables.RATT1;
import static com.fmotech.chessy.MoveTables.RATT2;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.Assert.assertEquals;


public class MoveGeneratorTest {

    private static final int SKIP = 1;

    @Test
    public void perftTests() {
        execute("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                20, 400, 8_902, 197_281, 4_865_609, 119_060_324, 3_195_901_860L);
        execute("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
                48, 2_039, 97_862, 4_085_603, 193_690_690, 8_031_647_685L);
        execute("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
                14, 191, 2_812, 43_238, 674_624, 11_030_083, 178_633_661);
        execute("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
                6, 264, 9_467, 422_333, 15_833_292, 706_045_033);
        execute("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
                44, 1_486, 62_379, 2_103_487, 89_941_194, 3_048_196_529L);
        execute("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
                46, 2_079, 89_890, 3_894_594, 164_075_551, 6_923_051_137L);
        execute("8/2p5/2Kp4/1P3k1r/1R3p2/8/4P1P1/8 b - - 0 1",
                16, 269, 4590, 80_683, 1_426_460, 25_913_670, 465_252_415);
        execute("K7/8/8/3Q4/4q3/8/8/7k b - - 0 1",
                6, 35, 495, 8349, 166_741, 3_370_175, 68_590_202, 1_389_464_081);
        execute("7K/8/8/4Q3/3q4/8/8/k7 b - - 0 1",
                6, 35, 495, 8349, 166_741, 3_370_175, 68_590_202, 1_389_464_081);
    }

    private void execute(String fen, long... counts) {
        System.out.println(fen);
        Board board = Board.load(fen);
        for (int i = 1; i <= counts.length - SKIP; i++) {
            LocalDateTime start = now();
            long count = MoveGenerator.countMoves(i, board, false);
            System.out.printf("%d: %10d in %6d ms\n", i, count, MILLIS.between(start, now()));
            assertEquals(String.valueOf(counts[i-1] - count), counts[i-1], count);
        }
    }

    @Test
    public void perftTestsOli() {
        executeOli("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                20, 400, 8_902, 197_281, 4_865_609, 119_060_324, 3_195_901_860L);
        executeOli("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
                48, 2_039, 97_862, 4_085_603, 193_690_690, 8_031_647_685L);
        executeOli("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
                14, 191, 2_812, 43_238, 674_624, 11_030_083, 178_633_661);
        executeOli("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
                6, 264, 9_467, 422_333, 15_833_292, 706_045_033);
        executeOli("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
                44, 1_486, 62_379, 2_103_487, 89_941_194, 3_048_196_529L);
        executeOli("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
                46, 2_079, 89_890, 3_894_594, 164_075_551, 6_923_051_137L);
        executeOli("8/2p5/2Kp4/1P3k1r/1R3p2/8/4P1P1/8 b - - 0 1",
                16, 269, 4590, 80_683, 1_426_460, 25_913_670, 465_252_415);
        executeOli("K7/8/8/3Q4/4q3/8/8/7k b - - 0 1",
                6, 35, 495, 8349, 166_741, 3_370_175, 68_590_202, 1_389_464_081);
        executeOli("7K/8/8/4Q3/3q4/8/8/k7 b - - 0 1",
                6, 35, 495, 8349, 166_741, 3_370_175, 68_590_202, 1_389_464_081);
    }

    private void executeOli(String fen, long... counts) {
        System.out.println(fen);
        for (int i = 1; i <= counts.length - SKIP; i++) {
            LocalDateTime start = now();
            long count = OliUtils.perft(fen, i);
            System.out.printf("%d: %10d in %6d ms\n", i, count, MILLIS.between(start, now()));
            assertEquals(String.valueOf(counts[i-1] - count), counts[i-1], count);
        }
    }

    @Test
    public void debug2() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
//        String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K1R1 b Qkq - 1 1";
        int depth = 3;

        long l = MoveGenerator.countMoves(depth, Board.load(fen), true);
        System.out.println(l);
        Map<String, Long> chessy = MoveGenerator.moves;
        Map<String, Long> oli = OliUtils.divide(fen, depth);

        if (!chessy.keySet().equals(oli.keySet())) {
            Set<String> tmp = new HashSet<>(chessy.keySet());
            tmp.removeAll(oli.keySet());
            System.out.println("In Chessy " + tmp);
            tmp = new HashSet<>(oli.keySet());
            tmp.removeAll(chessy.keySet());
            System.out.println("In Oli " + tmp);
        }

        for (String move : chessy.keySet()) {
            if (oli.getOrDefault(move, -1L) != (long) chessy.getOrDefault(move, -2L)) {
                System.out.println(move + " Expected: " + oli.get(move) + " vs " + chessy.get(move));
            }
        }
    }

    @Test
    public void testMask() {
        for (int j = 0; j < 64; j++) {
            for (int i = 0; i < 64; i++) {
                if (i == j) continue;
                if ((RATT1(i, 0) & RATT1(j, 0)) != 0) {
                    assertEquals(i + " " + j, 1, DIR(i, j));
                }
                if ((RATT2(i, 0) & RATT2(j, 0)) != 0) {
                    assertEquals(i + " " + j, 2, DIR(i, j));
                }
//                if ((BATT3(i, 0) & BATT3(j, 0)) != 0) {
//                    assertEquals(i + " " + j, 3, DIR(i, j));
//                }
//                if ((BATT4(i, 0) & BATT4(j, 0)) != 0) {
//                    assertEquals(i + " " + j, 4, DIR(i, j));
//                }
            }
        }
    }
}
