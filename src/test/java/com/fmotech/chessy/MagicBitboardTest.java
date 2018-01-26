package com.fmotech.chessy;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static com.fmotech.chessy.BitOperations.bitCount;
import static com.fmotech.chessy.BitOperations.permutateBoard;
import static com.fmotech.chessy.BitOperations.southFill;
import static com.fmotech.chessy.KoggeStone.E;
import static com.fmotech.chessy.KoggeStone.N;
import static com.fmotech.chessy.KoggeStone.NE;
import static com.fmotech.chessy.KoggeStone.NW;
import static com.fmotech.chessy.KoggeStone.S;
import static com.fmotech.chessy.KoggeStone.SE;
import static com.fmotech.chessy.KoggeStone.SW;
import static com.fmotech.chessy.KoggeStone.W;
import static com.fmotech.chessy.KoggeStone.slide;
import static java.time.LocalDateTime.now;
import static java.util.stream.IntStream.range;

public class MagicBitboardTest {

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
        System.out.println(Arrays.toString(indexes));

        int[] freq = new int[49];
        for (int i = 0; i < indexes.length; i++) {
            freq[indexes[i]]++;
        }
        System.out.println(Arrays.toString(freq));
        for (int i = 0; i < indexes.length; i++) {
            if (freq[indexes[i]] == 1) System.out.println(i);
        }

        long mask = (1L << 49) - 1;
        for (int i = 0; i < indexes.length && mask != 0; i++) {
            mask &= ~(1L << indexes[i]);
            if (mask == 0) {
                System.out.println(indexes[i] + " pos " + i);
            }
        }


        LocalDateTime start = LocalDateTime.now();
        System.out.println(Arrays.toString(indexes));

        AtomicLong next = new AtomicLong(10000);
        AtomicLong total = new AtomicLong(0);
        IntStream.range(1, 4).parallel().forEach(i -> {
            long high = (1 << i) - 1;
            long maxHigh = next(high << (32 - i));
            while (high != maxHigh) {
//                if (total.get() > next.get()) {
//                    System.out.println(total);
//                    next.addAndGet(10000);
//                }
                long h = high;
                for (int j = 1; j < 6; j++) {
                    long[] usedBy = new long[1 << 11];
                    long low = (1 << j) - 1;
                    long maxLow = next(low << (32 - j));
                    while (low != maxLow) {
                        long n = ~(h << 32 | low);
                        if (validateFancy(0, 12, indexes, n)) {
                            System.out.println(DebugUtils.toHexString(n));
                            System.exit(0);
                        }
                        total.addAndGet(1);
                        low = next(low);
                    }
                }
                high = next(high);
            }
            System.out.println(ChronoUnit.MILLIS.between(start, LocalDateTime.now()));
        });

        System.out.println(total);
        System.out.println(ChronoUnit.MILLIS.between(start, LocalDateTime.now()));
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