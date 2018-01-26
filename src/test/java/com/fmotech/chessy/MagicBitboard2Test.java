package com.fmotech.chessy;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.fmotech.chessy.BitOperations.bitCount;
import static com.fmotech.chessy.BitOperations.permutateBoard;
import static com.fmotech.chessy.KoggeStone.E;
import static com.fmotech.chessy.KoggeStone.N;
import static com.fmotech.chessy.KoggeStone.NE;
import static com.fmotech.chessy.KoggeStone.NW;
import static com.fmotech.chessy.KoggeStone.S;
import static com.fmotech.chessy.KoggeStone.SE;
import static com.fmotech.chessy.KoggeStone.SW;
import static com.fmotech.chessy.KoggeStone.W;
import static com.fmotech.chessy.KoggeStone.slide;
import static java.util.stream.IntStream.range;

public class MagicBitboard2Test {

    private static final long[] ROOK_MASK = range(0, 64).mapToLong(i -> 1L << i)
            .map(b -> (slide(b, 0, N) & 0x00ffffffffffffffL)
                    | (slide(b, 0, S) & 0xffffffffffffff00L)
                    | (slide(b, 0, E) & 0x7f7f7f7f7f7f7f7fL)
                    | (slide(b, 0, W) & 0xfefefefefefefefeL)).toArray();

    private static final long[] BISHOP_MASK = range(0, 64).mapToLong(i -> 1L << i)
            .map(b -> (slide(b, 0, NE) & 0x007e7e7e7e7e7e00L)
                    | (slide(b, 0, SE) & 0x007e7e7e7e7e7e00L)
                    | (slide(b, 0, NW) & 0x007e7e7e7e7e7e00L)
                    | (slide(b, 0, SW) & 0x007e7e7e7e7e7e00L)).toArray();

    private static final long[][] ROOK_PERMUTATIONS = initializePermutations(ROOK_MASK);
    private static final long[][] BISHOP_PERMUTATIONS = initializePermutations(BISHOP_MASK);

    private static final long[][] ROOK_ATTACKS = initializeRookAttacks(ROOK_PERMUTATIONS, N, S, E, W);
    private static final long[][] BISHOP_ATTACKS = initializeRookAttacks(ROOK_PERMUTATIONS, N, S, E, W);

    private static final long[][] DATA = createData();

    private static long[][] createData() {
        Map<Long, Byte> map = new HashMap<>();
        final int sq = 0;
        byte c = 0;
        for (int i = 0; i < ROOK_ATTACKS[sq].length; i++) {
            if (!map.containsKey(ROOK_ATTACKS[sq][i])) {
                map.put(ROOK_ATTACKS[sq][i], c++);
            }
        }
        byte[] indexes = new byte[ROOK_ATTACKS[sq].length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = map.get(ROOK_ATTACKS[sq][i]);
        }

        Map<Byte, List<Long>> collect = IntStream.range(0, 1 << 12).mapToObj(i -> ImmutablePair.of(i, ROOK_PERMUTATIONS[sq][i]))
                .collect(Collectors.groupingBy(e -> map.get(ROOK_ATTACKS[sq][e.left]), Collectors.mapping(e -> e.right, Collectors.toList())));
        return collect.values().stream().sorted(Comparator.comparing(e -> -e.size())).map(e -> e.stream().mapToLong(x -> x).toArray()).toArray(long[][]::new);
    }

    private static long[][] initializePermutations(long... mask) {
        long[][] permutations = new long[mask.length][];
        for (int sq = 0; sq < mask.length; sq++) {
            int numBits = BitOperations.bitCount(mask[sq]);
            permutations[sq] = new long[1 << numBits];
            for (int i = 0; i < 1 << numBits; i++) {
                permutations[sq][i] = permutateBoard(numBits, i, mask[sq]);
            }
        }
        return permutations;
    }

    private static long[][] initializeRookAttacks(long[][] permutations, int dir1, int dir2, int dir3, int dir4) {
        long[][] attacks = new long[64][];
        for (int sq = 0; sq < 64; sq++) {
            long b = 1L << sq;
            attacks[sq] = new long[permutations[sq].length];
            for (int i = 0; i < permutations[sq].length; i++) {
                attacks[sq][i] =  slide(b, permutations[sq][i], dir1)
                    | slide(b, permutations[sq][i], dir2)
                    | slide(b, permutations[sq][i], dir3)
                    | slide(b, permutations[sq][i], dir4);
            }
        }
        return attacks;
    }

    public static class Bits {
        private static final int SIZE = 32;

        private long[] prev = new long[SIZE];
        private long[] next = new long[SIZE];

        public void clear() {
            Arrays.fill(prev, 0);
            Arrays.fill(next, 0);
        }

        public void set(int n) {
            int index = n >>> 6;
            long bit = 1L << n;
            next[index] |= bit;
        }

        public boolean checkSet(int n) {
            int index = n >>> 6;
            long bit = 1L << n;
            next[index] |= bit;
            return (prev[index] & bit) != 0;
        }

        public void next() {
            System.arraycopy(next, 0, prev, 0, SIZE);
        }
    }

    public static void main(String[] args) {
//        LocalDateTime time = now();
//        List<Long> magics = new ArrayList<>();
//        for (int i = 0; i < 64; i++) {
//            magics.add(generateMagicFor(i, ROOK_MASK, ROOK_PERMUTATIONS));
//        }
//        for (int i = 0; i < 64; i++) {
//            magics.add(generateMagicFor(i, BISHOP_MASK, BISHOP_PERMUTATIONS));
//        }
//        for (int i = 0; i < 128; i++) {
//            System.out.print("0x" + DebugUtils.toHexString(magics.get(i)) + "L, ");
//            if (i % 4 == 3)
//                System.out.println();
//            if (i % 64 == 63)
//                System.out.println();
//        }
//        System.out.println(ChronoUnit.MILLIS.between(time, now()));

//        System.out.println(Arrays.deepToString(longs));


        LocalDateTime start = LocalDateTime.now();

        List<ImmutablePair<Integer, Integer>> list = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            for (int j = 1; j <= 6; j++) {
                list.add(ImmutablePair.of(i, j));
            }
        }

        list.stream().parallel().forEach(e -> {
            int i = e.left;
            int j = e.right;
            Bits bits = new Bits();
            long high = (1 << i) - 1;
            long maxHigh = next(high << (32 - i));
            while (high != maxHigh) {
                long h = high;
                long low = (1 << j) - 1;
                long maxLow = next(low << (32 - j));
                while (low != maxLow) {
                    long n = ~(h << 32 | low);
                    if (tryFancy(DATA, bits, n)) {
                        System.out.println(DebugUtils.toHexString(n));
                        System.exit(0);
                    }
                    low = next(low);
                }
                high = next(high);
            }
            System.out.println((ChronoUnit.MILLIS.between(start, LocalDateTime.now())) + " level " + i + " " + j + " " + Arrays.toString(failure));
        });

        System.out.println();
    }

    static long[] failure = new long[DATA.length];

    private static boolean tryFancy(long[][] longs, Bits bits, long magic) {
        bits.clear();
        for (long p : longs[0])
            bits.set((int) ((magic * p) >>> 53));
        bits.next();
        for (int j = 1; j < longs.length; j++) {
            for (int i = 0; i < longs[j].length; i++) {
                if (bits.checkSet((int) ((magic * longs[j][i]) >>> 53))) {
                    failure[j] += 1;
                    return false;
                }
            }
            bits.next();
        }
        return true;
    }

    static long generateMagicFor(int sq, long[] mask, long[][] permutations) {
        int bitCount = bitCount(mask[sq]);
        BitSet usedBy = new BitSet();
        for (int i = 1; i <= 6; i++) {
            long n = (1 << i) - 1;
            long end = next(n << (64 - i));
            while (n != end) {
                if (validate(n, sq, bitCount, usedBy, permutations)) {
                    System.out.println("Magic for " + sq + ": " + DebugUtils.toHexString(n));
                    return n;
                }
                n = next(n);
            }
        }
        throw new IllegalStateException("No magic for sq " + sq);
    }

    static boolean validate(long magicNumber, int sq, int bitCount, BitSet usedBy, long[][] permutations) {
        usedBy.clear();
        for (int i = 1; i < permutations[sq].length; i++) {
            long p = permutations[sq][i];
            int index = (int) ((p * magicNumber) >>> (64 - bitCount));
            if (usedBy.get(index)) return false;
            usedBy.set(index);
        }
        return !usedBy.get(0);
    }

    static final long[] empty = new long[1 << 11];

    static boolean validateFancy(int pos, int bitCount, byte[] indexes, long magicNumber) {
//        System.arraycopy(empty, 0, usedBy, 0, usedBy.length);
        long mask = (1L << 49) - 1;
        long[] permutation = ROOK_PERMUTATIONS[pos];
        for (int i = 0; i < permutation.length; i++) {
            long p = permutation[i];
            int index = (int) ((p * magicNumber) >>> (65 - bitCount));
            mask &= ~(1L << indexes[index]);
        }
        return mask == 0;
    }

    static boolean validateFancy(int pos, int bitCount, long[] usedBy, long magicNumber) {
//        System.arraycopy(empty, 0, usedBy, 0, usedBy.length);
        Arrays.fill(usedBy, 0);
        long[] permutation = ROOK_PERMUTATIONS[pos];
        for (int i = 0; i < permutation.length; i++) {
            long p = permutation[i];
            long ray = ROOK_ATTACKS[pos][i];
            int index = (int) ((p * magicNumber) >>> (65 - bitCount));
            if (usedBy[index] != 0 && usedBy[index] != ray) return false;
            usedBy[index] = ray;
        }
        return true;
    }

    private static long next(long x) {
        long smallest = x & -x;
        long ripple = x + smallest;
        long ones = x ^ ripple;
        ones = (ones >>> 2) / smallest;
        return ripple | ones;
    }
}
/*
/usr/java/jdk1.8.0_121/bin/java -Dvisualvm.id=833191828897 -javaagent:/home/modesto/idea-IC-172.3317.76/lib/idea_rt.jar=43848:/home/modesto/idea-IC-172.3317.76/bin -Dfile.encoding=UTF-8 -classpath /usr/java/jdk1.8.0_121/jre/lib/charsets.jar:/usr/java/jdk1.8.0_121/jre/lib/deploy.jar:/usr/java/jdk1.8.0_121/jre/lib/ext/cldrdata.jar:/usr/java/jdk1.8.0_121/jre/lib/ext/dnsns.jar:/usr/java/jdk1.8.0_121/jre/lib/ext/jaccess.jar:/usr/java/jdk1.8.0_121/jre/lib/ext/jfxrt.jar:/usr/java/jdk1.8.0_121/jre/lib/ext/localedata.jar:/usr/java/jdk1.8.0_121/jre/lib/ext/nashorn.jar:/usr/java/jdk1.8.0_121/jre/lib/ext/sunec.jar:/usr/java/jdk1.8.0_121/jre/lib/ext/sunjce_provider.jar:/usr/java/jdk1.8.0_121/jre/lib/ext/sunpkcs11.jar:/usr/java/jdk1.8.0_121/jre/lib/ext/zipfs.jar:/usr/java/jdk1.8.0_121/jre/lib/javaws.jar:/usr/java/jdk1.8.0_121/jre/lib/jce.jar:/usr/java/jdk1.8.0_121/jre/lib/jfr.jar:/usr/java/jdk1.8.0_121/jre/lib/jfxswt.jar:/usr/java/jdk1.8.0_121/jre/lib/jsse.jar:/usr/java/jdk1.8.0_121/jre/lib/management-agent.jar:/usr/java/jdk1.8.0_121/jre/lib/plugin.jar:/usr/java/jdk1.8.0_121/jre/lib/resources.jar:/usr/java/jdk1.8.0_121/jre/lib/rt.jar:/home/modesto/Projects/chessy/target/test-classes:/home/modesto/Projects/chessy/target/classes:/home/modesto/.m2/repository/junit/junit/4.12/junit-4.12.jar:/home/modesto/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:/home/modesto/.m2/repository/org/apache/commons/commons-lang3/3.6/commons-lang3-3.6.jar com.fmotech.chessy.MagicBitboard2Test
997 level 1 3 [0, 1344571, 34560, 2790, 0, 5326, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
6127 level 1 4 [0, 7771964, 284986, 20753, 0, 31149, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
6192 level 1 2 [0, 7859994, 288399, 21036, 0, 31988, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
6196 level 1 1 [0, 7866595, 288614, 21050, 0, 32075, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
103113 level 1 6 [0, 150448827, 6464131, 328235, 0, 348201, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
128085 level 1 5 [0, 187914985, 9180151, 848166, 433, 649239, 6277, 0, 0, 479, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
129238 level 2 2 [0, 189833052, 9409662, 889019, 579, 691663, 6891, 0, 0, 1612, 7, 0, 0, 0, 137, 269, 2, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
139920 level 2 3 [0, 207604123, 10939620, 1189800, 2837, 913917, 32479, 88, 0, 11119, 1039, 0, 0, 1, 713, 896, 16, 0, 0, 0, 0, 0, 26, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
139996 level 2 1 [0, 207726852, 10946459, 1192800, 2844, 918195, 32526, 88, 0, 11290, 1039, 0, 0, 1, 728, 903, 16, 0, 0, 0, 0, 0, 26, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
247616 level 3 3 [0, 370653898, 35886085, 4328675, 68876, 4564006, 284389, 737, 2923, 285662, 15484, 22, 10, 212, 26777, 17097, 1239, 44, 0, 0, 0, 0, 2396, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
259642 level 3 2 [0, 388367902, 39476787, 4501615, 72718, 4948684, 301874, 763, 3331, 322198, 16530, 24, 12, 243, 30254, 18851, 1275, 44, 0, 0, 0, 0, 2457, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
392911 level 2 5 [0, 587283322, 71556452, 6930297, 134697, 7663002, 499920, 1791, 5844, 551332, 49020, 98, 134, 521, 90611, 32828, 1650, 588, 0, 0, 0, 0, 3052, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
463893 level 2 4 [0, 695667080, 88444021, 8549131, 185210, 9674256, 635845, 2889, 11336, 802949, 64854, 164, 349, 1307, 144020, 48902, 3225, 588, 0, 0, 0, 1, 3754, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
749150 level 3 4 [0, 1134947855, 153389292, 16452332, 395720, 16618086, 1278684, 7939, 29205, 1528084, 115943, 536, 1011, 3536, 399631, 127232, 4312, 1303, 0, 0, 1, 3, 6369, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
1716300 level 2 6 [0, 2631922884, 382495237, 36772118, 1113786, 33921016, 4074791, 32867, 122567, 4631176, 791533, 2484, 4465, 14135, 1446445, 427981, 14030, 2746, 0, 0, 1, 83, 16534, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
1717150 level 3 1 [0, 2633367929, 382667464, 36788087, 1114060, 33950555, 4077215, 32891, 122630, 4640195, 791776, 2484, 4465, 14140, 1448217, 428498, 14035, 2750, 0, 0, 1, 83, 16540, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
2262976 level 6 2 [0, 3510089765, 458686341, 44590030, 1514468, 41952562, 5532426, 46806, 190676, 6907185, 1127085, 3210, 6696, 20499, 2019397, 608873, 19500, 3430, 0, 0, 1, 146, 21053, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
6075737 level 3 5 [0, 9795694559, 1168149762, 101475971, 2966873, 81869599, 9883169, 74741, 276804, 11158650, 1623324, 4688, 9035, 28012, 3163702, 1163873, 77235, 12784, 0, 3, 2, 159, 54161, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
6834025 level 4 3 [0, 11036020410, 1374687135, 117804311, 3508981, 95299269, 12601828, 87424, 327483, 13692530, 3330913, 5620, 9715, 33214, 3513268, 1316139, 85280, 17669, 0, 3, 2, 159, 63433, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
11909587 level 4 4 [0, 19408287214, 2892882344, 234418159, 6974450, 179265807, 27795511, 148901, 577177, 26370225, 5200324, 9790, 13745, 50562, 5358237, 2198860, 204085, 42413, 4, 4, 3, 304, 132399, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
11995722 level 4 2 [0, 19545547266, 2919884885, 236249188, 7061699, 181602822, 28008569, 150576, 587469, 26794241, 5229578, 9865, 13921, 51272, 5430627, 2228770, 204720, 42465, 4, 4, 3, 305, 134283, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
12002041 level 4 1 [0, 19556103790, 2922019279, 236402668, 7069089, 181817714, 28026402, 150776, 588935, 26859081, 5232104, 9890, 13957, 51378, 5445529, 2232292, 204729, 42465, 4, 4, 3, 305, 134310, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
17276650 level 3 6 [0, 28727329608, 4515719437, 366488436, 10078686, 263803587, 43364414, 207340, 834417, 36530392, 7247045, 12945, 18590, 69902, 7245681, 3073558, 282306, 57650, 4, 6, 3, 440, 183752, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
17340264 level 6 3 [0, 28837598735, 4534481354, 367814480, 10125822, 265334655, 43577366, 208458, 839746, 36811714, 7277560, 12989, 18713, 70381, 7296833, 3091489, 282826, 57694, 4, 6, 3, 440, 184601, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
17735048 level 5 2 [0, 29501146038, 4633727978, 376342917, 10460070, 271850226, 45055774, 218593, 887947, 38637930, 7483071, 13459, 19955, 74032, 7679982, 3220958, 286433, 58270, 4, 6, 3, 470, 189020, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
21789194 level 5 3 [0, 36721513821, 5823558829, 460849903, 12758879, 323558312, 54279930, 266680, 1094130, 47663087, 9128722, 15969, 23715, 88077, 9551386, 4255020, 446613, 85273, 10, 15, 6, 556, 270928, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
21825410 level 5 1 [0, 36780844502, 5836059128, 461959965, 12790297, 324401709, 54479825, 268561, 1102377, 48009535, 9154824, 16180, 24148, 88893, 9675525, 4276314, 446654, 85273, 10, 15, 6, 560, 271083, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
21981845 level 6 1 [0, 37053952390, 5886520430, 467278538, 12952138, 327881881, 55302571, 279898, 1137697, 49357409, 9273322, 17268, 26928, 94123, 10279530, 4353476, 446752, 85273, 10, 15, 6, 586, 271648, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

 */