package com.fmotech.chessy;

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

public class MagicBitboard {

    private static final long[] ROOK_MAGIC = new long[] {
            0xa180022080400230L, 0x0040100040022000L, 0x0080088020001002L, 0x0080080280841000L,
            0x4200042010460008L, 0x04800a0003040080L, 0x0400110082041008L, 0x008000a041000880L,
            0x10138001a080c010L, 0x0000804008200480L, 0x00010011012000c0L, 0x0022004128102200L,
            0x000200081201200cL, 0x202a001048460004L, 0x0081000100420004L, 0x4000800380004500L,
            0x0000208002904001L, 0x0090004040026008L, 0x0208808010002001L, 0x2002020020704940L,
            0x8048010008110005L, 0x6820808004002200L, 0x0a80040008023011L, 0x00b1460000811044L,
            0x4204400080008ea0L, 0xb002400180200184L, 0x2020200080100380L, 0x0010080080100080L,
            0x2204080080800400L, 0x0000a40080360080L, 0x02040604002810b1L, 0x008c218600004104L,
            0x8180004000402000L, 0x488c402000401001L, 0x4018a00080801004L, 0x1230002105001008L,
            0x8904800800800400L, 0x0042000c42003810L, 0x008408110400b012L, 0x0018086182000401L,
            0x2240088020c28000L, 0x001001201040c004L, 0x0a02008010420020L, 0x0010003009010060L,
            0x0004008008008014L, 0x0080020004008080L, 0x0282020001008080L, 0x50000181204a0004L,
            0x0102042111804200L, 0x40002010004001c0L, 0x0019220045508200L, 0x020030010060a900L,
            0x0008018028040080L, 0x0088240002008080L, 0x0010301802830400L, 0x00332a4081140200L,
            0x008080010a601241L, 0x0001008010400021L, 0x0004082001007241L, 0x0211009001200509L,
            0x8015001002441801L, 0x0801000804000603L, 0x0c0900220024a401L, 0x0001000200608243L
    };
    private static final long[] BISHOP_MAGIC = new long[] {
            0x2910054208004104L, 0x02100630a7020180L, 0x5822022042000000L, 0x2ca804a100200020L,
            0x0204042200000900L, 0x2002121024000002L, 0x80404104202000e8L, 0x812a020205010840L,
            0x8005181184080048L, 0x1001c20208010101L, 0x1001080204002100L, 0x1810080489021800L,
            0x0062040420010a00L, 0x5028043004300020L, 0xc0080a4402605002L, 0x08a00a0104220200L,
            0x0940000410821212L, 0x001808024a280210L, 0x040c0422080a0598L, 0x4228020082004050L,
            0x0200800400e00100L, 0x020b001230021040L, 0x00090a0201900c00L, 0x004940120a0a0108L,
            0x0020208050a42180L, 0x001004804b280200L, 0x2048020024040010L, 0x0102c04004010200L,
            0x020408204c002010L, 0x02411100020080c1L, 0x102a008084042100L, 0x0941030000a09846L,
            0x0244100800400200L, 0x4000901010080696L, 0x0000280404180020L, 0x0800042008240100L,
            0x0220008400088020L, 0x04020182000904c9L, 0x0023010400020600L, 0x0041040020110302L,
            0x0412101004020818L, 0x8022080a09404208L, 0x1401210240484800L, 0x0022244208010080L,
            0x1105040104000210L, 0x2040088800c40081L, 0x8184810252000400L, 0x4004610041002200L,
            0x040201a444400810L, 0x4611010802020008L, 0x80000b0401040402L, 0x0020004821880a00L,
            0x8200002022440100L, 0x0009431801010068L, 0x1040c20806108040L, 0x0804901403022a40L,
            0x2400202602104000L, 0x0208520209440204L, 0x040c000022013020L, 0x2000104000420600L,
            0x0400000260142410L, 0x0800633408100500L, 0x00002404080a1410L, 0x0138200122002900L
    };

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

    private static final long[] ROOK_RAY = initializeRays(ROOK_MASK, ROOK_MAGIC, false, N, S, W, E);
    private static final long[] ROOK_XRAY = initializeRays(ROOK_MASK, ROOK_MAGIC, true, N, S, W, E);
    private static final long[] BISHOP_RAY = initializeRays(BISHOP_MASK, BISHOP_MAGIC, false, NE, SE, NW, SW);
    private static final long[] BISHOP_XRAY = initializeRays(BISHOP_MASK, BISHOP_MAGIC, true, NE, SE, NW, SW);

    private static long[] initializeRays(long[] mask, long[] magic, boolean xRays, int dir1, int dir2, int dir3, int dir4) {
        long[] rays = new long[0x40000];
        for (int sq = 0; sq < 64; sq++) {
            int numBits = BitOperations.bitCount(mask[sq]);
            for (int i = 0; i < 1 << numBits; i++) {
                long permutation = permutateBoard(numBits, i, mask[sq]);
                int index = sq << 12 | (int) ((magic[sq] * permutation) >>> 52);
                long ray = slide(1L << sq, permutation, dir1)
                        | slide(1L << sq, permutation, dir2)
                        | slide(1L << sq, permutation, dir3)
                        | slide(1L << sq, permutation, dir4);
                permutation &= ~ray;
                long xray = (slide(1L << sq, permutation, dir1)
                        | slide(1L << sq, permutation, dir2)
                        | slide(1L << sq, permutation, dir3)
                        | slide(1L << sq, permutation, dir4)) & ~ray;
                int garbage = (1 << (12 - numBits)) - 1;
                for (int g = 0; g <= garbage; g++) {
                    rays[index & ~garbage | g] = xRays ? xray : ray;
                }
            }
        }
        return rays;
    }

    public static long rookRay(int position, long bitBoard) {
        return ROOK_RAY[position << 12 | (int) (((bitBoard & ROOK_MASK[position]) * ROOK_MAGIC[position]) >>> 52)];
    }

    public static long rookXRay(int position, long bitBoard) {
        return ROOK_XRAY[position << 12 | (int) (((bitBoard & ROOK_MASK[position]) * ROOK_MAGIC[position]) >>> 52)];
    }

    public static long bishopRay(int position, long bitBoard) {
        return BISHOP_RAY[position << 12 | (int) (((bitBoard & BISHOP_MASK[position]) * BISHOP_MAGIC[position]) >>> 52)];
    }

    public static long bishopXRay(int position, long bitBoard) {
        return BISHOP_XRAY[position << 12 | (int) (((bitBoard & BISHOP_MASK[position]) * BISHOP_MAGIC[position]) >>> 52)];
    }

    public static long MASK(int from, int to) {
        int change = from ^ to;
        return (change & 56) == 0 || (change & 7) == 0 ? ROOK_RAY[from << 12] & ROOK_RAY[to << 12] : BISHOP_RAY[from << 12] & BISHOP_RAY[to << 12];
    }

    public static long SEGMENT(int from, int to, long bitBoard) {
        int change = from ^ to;
        return (change & 56) == 0 || (change & 7) == 0 ? rookRay(from, bitBoard) & rookRay(to, bitBoard) : bishopRay(from, bitBoard) & bishopRay(to, bitBoard);
    }
}
