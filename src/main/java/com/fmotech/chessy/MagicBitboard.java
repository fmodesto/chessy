package com.fmotech.chessy;

import static com.fmotech.chessy.BitOperations.permutateBoard;
import static com.fmotech.chessy.Board.WHITE;
import static com.fmotech.chessy.BoardUtils.BIT;
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
import static java.util.stream.IntStream.range;

public class MagicBitboard {

    private static final long[] ROOK_MAGIC = new long[]{
            0x0080001020400080L, 0x0040001000200040L, 0x0080081000200080L, 0x0080040800100080L,
            0x0080020400080080L, 0x0080010200040080L, 0x0080008001000200L, 0x0080002040800100L,
            0x0000800020400080L, 0x0000400020005000L, 0x0000801000200080L, 0x0000800800100080L,
            0x0000800400080080L, 0x0000800200040080L, 0x0000800100020080L, 0x0000800040800100L,
            0x0000208000400080L, 0x0000404000201000L, 0x0000808010002000L, 0x0000808008001000L,
            0x0000808004000800L, 0x0000808002000400L, 0x0000010100020004L, 0x0000020000408104L,
            0x0000208080004000L, 0x0000200040005000L, 0x0000100080200080L, 0x0000080080100080L,
            0x0000040080080080L, 0x0000020080040080L, 0x0000010080800200L, 0x0000800080004100L,
            0x0000204000800080L, 0x0000200040401000L, 0x0000100080802000L, 0x0000080080801000L,
            0x0000040080800800L, 0x0000020080800400L, 0x0000020001010004L, 0x0000800040800100L,
            0x0000204000808000L, 0x0000200040008080L, 0x0000100020008080L, 0x0000080010008080L,
            0x0000040008008080L, 0x0000020004008080L, 0x0000010002008080L, 0x0000004081020004L,
            0x0000204000800080L, 0x0000200040008080L, 0x0000100020008080L, 0x0000080010008080L,
            0x0000040008008080L, 0x0000020004008080L, 0x0000800100020080L, 0x0000800041000080L,
            0x0000102040800101L, 0x0000102040008101L, 0x0000081020004101L, 0x0000040810002101L,
            0x0001000204080011L, 0x0001000204000801L, 0x0001000082000401L, 0x0000002040810402L
    };
    private static final long[] BISHOP_MAGIC = new long[]{
            0x0002020202020200L, 0x0002020202020000L, 0x0004010202000000L, 0x0004040080000000L,
            0x0001104000000000L, 0x0000821040000000L, 0x0000410410400000L, 0x0000104104104000L,
            0x0000040404040400L, 0x0000020202020200L, 0x0000040102020000L, 0x0000040400800000L,
            0x0000011040000000L, 0x0000008210400000L, 0x0000004104104000L, 0x0000002082082000L,
            0x0004000808080800L, 0x0002000404040400L, 0x0001000202020200L, 0x0000800802004000L,
            0x0000800400a00000L, 0x0000200100884000L, 0x0000400082082000L, 0x0000200041041000L,
            0x0002080010101000L, 0x0001040008080800L, 0x0000208004010400L, 0x0000404004010200L,
            0x0000840000802000L, 0x0000404002011000L, 0x0000808001041000L, 0x0000404000820800L,
            0x0001041000202000L, 0x0000820800101000L, 0x0000104400080800L, 0x0000020080080080L,
            0x0000404040040100L, 0x0000808100020100L, 0x0001010100020800L, 0x0000808080010400L,
            0x0000820820004000L, 0x0000410410002000L, 0x0000082088001000L, 0x0000002011000800L,
            0x0000080100400400L, 0x0001010101000200L, 0x0002020202000400L, 0x0001010101000200L,
            0x0000410410400000L, 0x0000208208200000L, 0x0000002084100000L, 0x0000000020880000L,
            0x0000001002020000L, 0x0000040408020000L, 0x0004040404040000L, 0x0002020202020000L,
            0x0000104104104000L, 0x0000002082082000L, 0x0000000020841000L, 0x0000000000208800L,
            0x0000000010020200L, 0x0000000404080200L, 0x0000040404040400L, 0x0002020202020200L
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

    private static final long[] ROOK_RAY = initializeRays(ROOK_MASK, ROOK_MAGIC, 12, false, N, S, W, E);
    private static final long[] ROOK_XRAY = initializeRays(ROOK_MASK, ROOK_MAGIC, 12, true, N, S, W, E);
    private static final long[] BISHOP_RAY = initializeRays(BISHOP_MASK, BISHOP_MAGIC, 9, false, NE, SE, NW, SW);
    private static final long[] BISHOP_XRAY = initializeRays(BISHOP_MASK, BISHOP_MAGIC, 9, true, NE, SE, NW, SW);

    private static final long[][] PAWN_ATTACK = new long[][]{
            range(0, 64).mapToLong(BoardUtils::BIT).map(b -> shiftOne(b, NW) | shiftOne(b, NE)).toArray(),
            range(0, 64).mapToLong(BoardUtils::BIT).map(b -> shiftOne(b, SW) | shiftOne(b, SE)).toArray()};
    private static final long[] KNIGHT = range(0, 64).mapToLong(i -> KoggeStone.knightMove(BIT(i))).toArray();
    private static final long[] KING = range(0, 64).mapToLong(i -> KoggeStone.kingMove(BIT(i))).toArray();

    private static long[] initializeRays(long[] mask, long[] magic, int bits, boolean xRays, int dir1, int dir2, int dir3, int dir4) {
        long[] rays = new long[64 << bits];
        for (int sq = 0; sq < 64; sq++) {
            int numBits = BitOperations.bitCount(mask[sq]);
            for (int i = 0; i < 1 << numBits; i++) {
                long permutation = permutateBoard(numBits, i, mask[sq]);
                int index = (sq << bits) | (int) ((magic[sq] * permutation) >>> (64 - bits));
                long ray = slide(1L << sq, permutation, dir1)
                        | slide(1L << sq, permutation, dir2)
                        | slide(1L << sq, permutation, dir3)
                        | slide(1L << sq, permutation, dir4);
                permutation &= ~ray;
                long xray = (slide(1L << sq, permutation, dir1)
                        | slide(1L << sq, permutation, dir2)
                        | slide(1L << sq, permutation, dir3)
                        | slide(1L << sq, permutation, dir4)) & ~ray;
                rays[index] = xRays ? xray : ray;
            }
        }
        return rays;
    }

    public static long MASK(int from, int to) {
        int change = from ^ to;
        return (change & 56) == 0 || (change & 7) == 0 ? ROOK_RAY[from << 12] & ROOK_RAY[to << 12] : BISHOP_RAY[from << 9] & BISHOP_RAY[to << 9];
    }

    public static long SEGMENT(int from, int to, long bitBoard) {
        int change = from ^ to;
        return (change & 56) == 0 || (change & 7) == 0 ? rookMove(from, bitBoard) & rookMove(to, bitBoard) : bishopMove(from, bitBoard) & bishopMove(to, bitBoard);
    }

    public static long rookXRay(int position, long bitBoard) {
        return ROOK_XRAY[position << 12 | (int) (((bitBoard & ROOK_MASK[position]) * ROOK_MAGIC[position]) >>> 52)];
    }

    public static long bishopXRay(int position, long bitBoard) {
        return BISHOP_XRAY[position << 9 | (int) (((bitBoard & BISHOP_MASK[position]) * BISHOP_MAGIC[position]) >>> 55)];
    }

    public static long pawnMove(int from, int sideToMove) {
        long pawn = BIT(from);
        return sideToMove == WHITE ? pawn << 8 : pawn >>> 8;
    }

    public static long pawnDoubleMove(int from, int sideToMove) {
        long pawn = BIT(from);
        return sideToMove == WHITE ? pawn << 16 : pawn >>> 16;
    }

    public static long pawnAttack(int from, int sideToMove) {
        return PAWN_ATTACK[sideToMove][from];
    }

    public static long knightMove(int from) {
        return KNIGHT[from];
    }

    public static long bishopMove(int position, long bitBoard) {
        return BISHOP_RAY[position << 9 | (int) (((bitBoard & BISHOP_MASK[position]) * BISHOP_MAGIC[position]) >>> 55)];
    }

    public static long rookMove(int position, long bitBoard) {
        return ROOK_RAY[position << 12 | (int) (((bitBoard & ROOK_MASK[position]) * ROOK_MAGIC[position]) >>> 52)];
    }

    public static long queenMove(int from, long occupancy) {
        return bishopMove(from, occupancy) | rookMove(from, occupancy);
    }

    public static long kingMove(int from) {
        return KING[from];
    }
}
