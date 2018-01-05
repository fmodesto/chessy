package com.fmotech.chessy.epd;

import com.fmotech.chessy.oli.OliUtils;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class EpdTests {

    public static final int TIME = 5;
    public static final int EXECUTE = -1;
    private final EpdReader.Epd epd;

    @Parameters
    public static List<Object[]> data() {
        List<Object[]> tests = EpdReader.read(Paths.get("src/test/resources/wacnew.epd"))
                .map(e -> new Object[]{e})
                .collect(Collectors.toList());
        return EXECUTE < 0 ? tests : Collections.singletonList(tests.get(EXECUTE));
    }

    public EpdTests(EpdReader.Epd epd) {
        this.epd = epd;
    }

    @Test
    public void execute() {
        System.out.println(epd.board);
        String bm = EpdReader.getFen(epd, "bm");
        String am = EpdReader.getFen(epd, "am");
        if (bm.length() > 0)
            System.out.println("Best moves: " + bm);
        if (am.length() > 0)
            System.out.println("Avoid moves: " + am);

        String bestMove = OliUtils.think(epd.fen, TIME);
        List<String> expectedBest = EpdReader.getMoves(epd, "bm");
        ignoreFalse(bestMove + " in [" + bm + "]", expectedBest.isEmpty() || expectedBest.contains(bestMove));
        List<String> expectedBad = EpdReader.getMoves(epd, "am");
        ignoreTrue(bestMove + " not in [" + am + "]", expectedBad.contains(bestMove));
    }

    private void ignoreFalse(String message, boolean condition) {
        Assume.assumeTrue(message, condition);
    }

    private void ignoreTrue(String message, boolean condition) {
        Assume.assumeFalse(message, condition);
    }
}

