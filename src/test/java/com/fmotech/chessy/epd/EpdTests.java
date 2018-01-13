package com.fmotech.chessy.epd;

import com.fmotech.chessy.Board;
import com.fmotech.chessy.Engine;
import com.fmotech.chessy.oli.OliThink;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class EpdTests {

    public static final int TIME = 90;
    public static final int EXECUTE = 179;
    public static final int DEPTH = 64;
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
        Engine.hashes.clear();
        OliThink.hashes.clear();
        System.out.println(epd.fen);
        String bm = EpdReader.getFen(epd, "bm");
        String am = EpdReader.getFen(epd, "am");
        if (bm.length() > 0)
            System.out.println("Best moves: " + bm);
        if (am.length() > 0)
            System.out.println("Avoid moves: " + am);

        String bestMove = think(epd.fen, TIME);
        List<String> expectedBest = EpdReader.getMoves(epd, "bm");
        ignoreFalse(bestMove + " in [" + bm + "]", expectedBest.isEmpty() || expectedBest.contains(bestMove));
        List<String> expectedBad = EpdReader.getMoves(epd, "am");
        ignoreTrue(bestMove + " not in [" + am + "]", expectedBad.contains(bestMove));
    }

    private String think(String fen, int time) {
        Engine engine = new Engine(Board.load(fen));
        String calc = engine.calc(time, DEPTH);
        System.out.println("Mine: " + calc);
//        String oli = OliUtils.think(fen, time, 64);
//        System.out.println("Oli: " + oli);
//        for (int i = 0; i < Math.min(OliThink.hashes.size(), Engine.hashes.size()); i++) {
//            if (OliThink.hashes.get(i) != (long) Engine.hashes.get(i)) {
//                System.err.println("FAILURE AT " + i);
//                throw new AssertionError("FAILURE AT " + i);
//            }
//        }
        assertEquals(OliThink.hashes, Engine.hashes);
//        for (int i = 0; i < 20; i++) {
//            Assert.assertArrayEquals("Iter: " + i, Engine.mem[i], OliThink.mem[i]);
//        }
//        assertEquals(oli, calc);
        return calc;
    }

    private void ignoreFalse(String message, boolean condition) {
        Assume.assumeTrue(message, condition);
    }

    private void ignoreTrue(String message, boolean condition) {
        Assume.assumeFalse(message, condition);
    }
}

/*
/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/bin/java -ea -Didea.test.cyclic.buffer.size=1048576 "-javaagent:/Applications/IntelliJ IDEA CE.app/Contents/lib/idea_rt.jar=56055:/Applications/IntelliJ IDEA CE.app/Contents/bin" -Dfile.encoding=UTF-8 -classpath "/Applications/IntelliJ IDEA CE.app/Contents/lib/idea_rt.jar:/Applications/IntelliJ IDEA CE.app/Contents/plugins/junit/lib/junit-rt.jar:/Applications/IntelliJ IDEA CE.app/Contents/plugins/junit/lib/junit5-rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/deploy.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/ext/cldrdata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/ext/dnsns.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/ext/jaccess.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/ext/jfxrt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/ext/localedata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/ext/nashorn.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/ext/sunec.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/ext/sunjce_provider.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/ext/sunpkcs11.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/ext/zipfs.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/javaws.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/jfxswt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/management-agent.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/plugin.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/lib/ant-javafx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/lib/dt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/lib/javafx-mx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/lib/jconsole.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/lib/packager.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/lib/sa-jdi.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/lib/tools.jar:/Users/fran/Projects/chessy/target/test-classes:/Users/fran/Projects/chessy/target/classes:/Users/fran/.m2/repository/junit/junit/4.12/junit-4.12.jar:/Users/fran/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:/Users/fran/.m2/repository/org/apache/commons/commons-lang3/3.6/commons-lang3-3.6.jar" com.intellij.rt.execution.junit.JUnitStarter -ideVersion5 -junit4 com.fmotech.chessy.epd.EpdTests,execute
objc[18443]: Class JavaLaunchHelper is implemented in both /Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/bin/java (0x10d9404c0) and /Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/libinstrument.dylib (0x10d9cc4e0). One of the two will be used. Which one is undefined.
r1b1kb1r/3q1ppp/pBp1pn2/8/Np3P2/5B2/PPP3PP/R2Q1RK1 w kq - 0 1
Best moves: f3c6
 1    11      0       227  a4c5
 2    57      1       820  a4c5 f8c5 b6c5
 3    82      2      2508  f3c6 f8c5 b6c5 d7c6
 4   587      4      5831  f3c6 f6d5 c6d7 c8d7 c2c3
 5   587      4      9557  f3c6 f6d5 c6d7 c8d7 c2c3 b4c3
 6   587      8     26359  f3c6 f6d5 c6d7 c8d7 c2c4 b4c3 b2c3
 7   325     14     58501  f3c6 f6d5 c6d7 c8d7 f4f5 d7a4 f5e6 d5b6
 8   348     20    124105  f3c6 f6d5 c6d7 c8d7 a4c5 d5b6 c5d7 b6d7 d1h5
 9   320     42    321309  f3c6 f6d5 c6d7 c8d7 c2c3 d5b6 a4b6 f8c5 g1h1 c5b6 c3b4
10   403    133   2693817  f3c6 f6d5 c6d7 c8d7 c2c3 d5b6 a4b6 f8c5 g1h1 c5b6 d1d6 b6e3
11   403    158   3502164  f3c6 f6d5 c6d7 c8d7 c2c3 d5b6 a4b6 f8c5 g1h1 c5b6 d1d6 b6e3 d6b4
12   397    228   5613543  f3c6 f6d5 c6d7 c8d7 f4f5 d5b6 a4b6 f8c5 g1h1 c5b6 d1d6 b6a5 d6e5 a5b6
13   407    337   9545128  f3c6 f6d5 c6d7 c8d7 f4f5 d5b6 a4b6 f8c5 g1h1 c5b6 d1d6 b6e3 f1f3 e3g5 f5e6
move f3c6

kibitz W: 407 Nodes: 6623046 QNodes: 2922082 Evals: 1465424 cs: 337 knps: 2824
Oli: f3c6
Mine:
64
 1    11      0       218  a4c5
 2    57      1       805  a4c5 f8c5 b6c5
 3    82      1      2377  f3c6 f8c5 b6c5 d7c6
 4   587      3      8203  f3c6 f6d5 c6d7 c8d7 c2c3
 5   587      4     15357  f3c6 f6d5 c6d7 c8d7 c2c3 b4c3
 6   587      6     40537  f3c6 f6d5 c6d7 c8d7 c2c4 b4c3 b2c3
 7   325     12    125705  f3c6 f6d5 c6d7 c8d7 f4f5 d7a4 f5e6 d5b6
 8   348     24    283902  f3c6 f6d5 c6d7 c8d7 a4c5 d5b6 c5d7 b6d7 d1h5
 9   320     47    778835  f3c6 f6d5 c6d7 c8d7 c2c3 d5b6 a4b6 f8c5 g1h1 c5b6 c3b4
10   403    108   2796312  f3c6 f6d5 c6d7 c8d7 c2c3 d5b6 a4b6 f8c5 g1h1 c5b6 d1d6 b6e3
11   403    151   4372134  f3c6 f6d5 c6d7 c8d7 c2c3 d5b6 a4b6 f8c5 g1h1 c5b6 d1d6 b6e3 d6b4
12   397    246   8011975  f3c6 f6d5 c6d7 c8d7 f4f5 d5b6 a4b6 f8c5 g1h1 c5b6 d1d6 b6a5 d6e5 a5b6
13   407    602  20678336  f3c6 f6d5 c6d7 c8d7 f4f5 d5b6 a4b6 f8c5 g1h1 c5b6 d1d6 b6e3 f1f3 e3g5 f5e6
move f3c6

kibitz W: 407 Nodes: 37159400 QNodes: 15697768 Evals: -1 s: 15001 knps: 3523

Process finished with exit code 0

 */