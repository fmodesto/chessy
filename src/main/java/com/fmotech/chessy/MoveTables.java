package com.fmotech.chessy;

import java.util.function.BiFunction;

import static com.fmotech.chessy.BitOperations.bitCount;
import static com.fmotech.chessy.BitOperations.lowestBit;
import static com.fmotech.chessy.KoggeStone.E;
import static com.fmotech.chessy.KoggeStone.N;
import static com.fmotech.chessy.KoggeStone.NE;
import static com.fmotech.chessy.KoggeStone.NW;
import static com.fmotech.chessy.KoggeStone.S;
import static com.fmotech.chessy.KoggeStone.SE;
import static com.fmotech.chessy.KoggeStone.SW;
import static com.fmotech.chessy.KoggeStone.W;
import static com.fmotech.chessy.KoggeStone.shiftOne;
import static com.fmotech.chessy.KoggeStone.slide;
import static com.fmotech.chessy.Utils.BIT;
import static java.util.stream.IntStream.range;

public class MoveTables {

    public static long RXRAY1(int pos, long occupied) { return RAYS[((pos) << 7) | key000(occupied, pos) | 0x8000]; }
    public static long RXRAY2(int pos, long occupied) { return RAYS[((pos) << 7) | key090(occupied, pos) | 0xA000]; }
    public static long BXRAY3(int pos, long occupied) { return RAYS[((pos) << 7) | key045(occupied, pos) | 0xC000]; }
    public static long BXRAY4(int pos, long occupied) { return RAYS[((pos) << 7) | key135(occupied, pos) | 0xE000]; }

    public static long RATT1(int pos, long occupied) { return RAYS[((pos) << 7) | key000(occupied, pos)]; }
    public static long RATT2(int pos, long occupied) { return RAYS[((pos) << 7) | key090(occupied, pos) | 0x2000]; }
    public static long BATT3(int pos, long occupied) { return RAYS[((pos) << 7) | key045(occupied, pos) | 0x4000]; }
    public static long BATT4(int pos, long occupied) { return RAYS[((pos) << 7) | key135(occupied, pos) | 0x6000]; }

    public static int DIR(int from, int to) {
        if (((from ^ to) & 56) == 0) return 1;
        if (((from ^ to) & 7) == 0) return 2;
        return ((from - to) % 9) == 0 ? 3 : 4;
    }

    public static long MASK(int from, int to) {
        if (((from ^ to) & 56) == 0) return RAYS[((from) << 7)];
        if (((from ^ to) & 7) == 0) return RAYS[((from) << 7) | 0x2000];
        return ((from - to) % 9) == 0 ? RAYS[((from) << 7) | 0x4000] : RAYS[((from) << 7) | 0x6000];
    }

    private static final int FILE_MASK = 0x07;
    private static final int RANK_MASK = 0x38;
    private static final long[] MASK_045 = range(0, 64).mapToLong(i -> createPermutationMask(BIT(i), SW, NE)).toArray();
    private static final long[] MASK_135 = range(0, 64).mapToLong(i -> createPermutationMask(BIT(i), SE, NW)).toArray();

    public static final long[] RAYS = initializeRays();
    public static final long[][] PAWN_ATTACK = new long[][] {
            range(0, 64).mapToLong(Utils::BIT).map(b -> shiftOne(b, NW) | shiftOne(b, NE)).toArray(),
            range(0, 64).mapToLong(Utils::BIT).map(b -> shiftOne(b, SW) | shiftOne(b, SE)).toArray() };
    public static final long[] KNIGHT = range(0, 64).mapToLong(i -> KoggeStone.knightMove(BIT(i))).toArray();
    public static final long[] KING = range(0, 64).mapToLong(i -> KoggeStone.kingMove(BIT(i))).toArray();

    private static long[] initializeRays() {
        long[] rays = new long[0x10000];
        initRays(rays, 0x0000, MoveTables::key000, W, E);
        initRays(rays, 0x2000, MoveTables::key090, N, S);
        initRays(rays, 0x4000, MoveTables::key045, SW, NE);
        initRays(rays, 0x6000, MoveTables::key135, SE, NW);
        return rays;
    }

    private static void initRays(long[] rays, int offset, BiFunction<Long, Integer, Integer> key, int dir1, int dir2) {
        for (int pos = 0; pos < 64; pos++) {
            long bit = BIT(pos);
            long permutationMask = createPermutationMask(bit, dir1, dir2);
            int numBits = bitCount(permutationMask);
            int numPermutations = 1 << numBits;
            for (int i = 0; i < numPermutations; i++) {
                long bitBoard = permutateBoard(numBits, i, permutationMask);
                long ray = slide(bit, bitBoard, dir1) | slide(bit, bitBoard, dir2);
                long xrayBoard = bitBoard & ~ray;
                long xray = (slide(bit, xrayBoard, dir1) | slide(bit, xrayBoard, dir2)) & xrayBoard;
                int index = key.apply(bitBoard, pos);
                rays[(pos << 7) + index + offset] = ray;
                rays[(pos << 7) + index + 0x8000 + offset] = xray;
            }
        }
    }

    private static long createPermutationMask(long bit, int dir1, int dir2) {
        return slide(bit, 0, dir1) | slide(bit, 0, dir2) | bit;
    }

    static long permutateBoard(int numBits, int permutationMask, long allSetBoard) {
        long permutation = allSetBoard;
        for (int i = 0; i < numBits; i++) {
            long bit = lowestBit(allSetBoard);
            if (((1 << i) & permutationMask) == 0)
                permutation ^= bit;
            allSetBoard ^= bit;
        }
        return permutation;
    }

    private static int key000(long bitBoard, int pos) {
        bitBoard >>= pos & RANK_MASK;
        return (int) (bitBoard & 0x7E);
    }

    private static int key045(long bitBoard, int pos) {
        return keyDiagonal(bitBoard & MASK_045[pos]);
    }

    private static int key090(long bitBoard, int pos) {
        bitBoard >>>= pos & FILE_MASK;
        bitBoard &= 0x0101010101010101L;
        bitBoard *= 0x0080402010080400L;
        return (int) (bitBoard >>> 57);
    }

    private static int key135(long bitBoard, int pos) {
        return keyDiagonal(bitBoard & MASK_135[pos]);
    }

    private static int keyDiagonal(long bitBoard) {
        bitBoard *= 0x0202020202020202L;
        return (int) (bitBoard >>> 57);
    }
}
