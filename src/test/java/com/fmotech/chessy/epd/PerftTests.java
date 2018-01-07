package com.fmotech.chessy.epd;

import com.fmotech.chessy.Board;
import com.fmotech.chessy.MoveGenerator;
import com.fmotech.chessy.epd.EpdReader.Epd;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class PerftTests {

    public static final int SKIP = 0;
    private final Epd epd;

    @Parameters
    public static List<Object[]> data() throws Exception {
        return EpdReader.read(Paths.get("src/test/resources/perftsuite.epd"))
                .map(e -> new Object[] { e })
                .collect(Collectors.toList());

    }

    public PerftTests(Epd epd) {
        this.epd = epd;
    }

    @Test
    public void execute() {
        System.out.println(epd.board);
        epd.actions.stream().limit(epd.actions.size() - SKIP).forEach(e -> execute(parseLong(e.parameter), epd.board, e.action.charAt(1) - '0'));
        System.out.println("Done");
    }

    private void execute(long expected, Board board, int level) {
        long hash = board.hash();
        int m0 = board.material(0);
        int m1 = board.material(1);
        int fifty = board.fifty();
        int ply = board.ply();
        LocalDateTime start = now();
        long count = MoveGenerator.countMoves(level, board, false);
        System.out.printf("%d: %10d in %6d ms\n", level, count, MILLIS.between(start, now()));
        assertEquals(expected, count);
        assertEquals("hash", hash, board.hash());
        assertEquals("white", m0, board.material(0));
        assertEquals("black", m1, board.material(1));
        assertEquals("fifty", fifty, board.fifty());
        assertEquals("ply", ply, board.ply());
    }
}
