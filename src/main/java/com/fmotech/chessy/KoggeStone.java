package com.fmotech.chessy;

import static com.fmotech.chessy.BitOperations.rotateLeft;

public class KoggeStone {

    public static final int N = 7, S = 3, E = 5, W = 1, NE = 6, SE = 0, NW = 4, SW = 2;

    private static int[] SHIFT = { -7, -1, -9, -8, 7, 1, 9, 8 };
    private static long[] AVOID_WRAP = {
            0x00fefefefefefefeL,
            0x7f7f7f7f7f7f7f7fL,
            0x007f7f7f7f7f7f7fL,
            0x00ffffffffffffffL,
            0x7f7f7f7f7f7f7f00L,
            0xfefefefefefefefeL,
            0xfefefefefefefe00L,
            0xffffffffffffff00L };

    public static long slide(long slider, long bitBoard, int dir8) {
        long fill = occludedFill(slider, ~bitBoard, dir8);
        return shiftOne(fill, dir8);
    }

    private static long occludedFill(long gen, long pro, int dir8) {
        int r = SHIFT[dir8]; // {+-1,7,8,9}
        pro &= AVOID_WRAP[dir8];
        gen |= pro & rotateLeft(gen, r);
        pro &= rotateLeft(pro, r);
        gen |= pro & rotateLeft(gen, 2 * r);
        pro &= rotateLeft(pro, 2 * r);
        gen |= pro & rotateLeft(gen, 4 * r);
        return gen;
    }

    private static long shiftOne(long b, int dir8) {
        int r = SHIFT[dir8]; // {+-1,7,8,9}
        return rotateLeft(b, r) & AVOID_WRAP[dir8];
    }

    public static long knightMove(long knight) {
        long l1 = (knight >>> 1) & 0x7f7f7f7f7f7f7f7fL;
        long l2 = (knight >>> 2) & 0x3f3f3f3f3f3f3f3fL;
        long r1 = (knight << 1) & 0xfefefefefefefefeL;
        long r2 = (knight << 2) & 0xfcfcfcfcfcfcfcfcL;
        long h1 = l1 | r1;
        long h2 = l2 | r2;
        return (h1 << 16) | (h1 >>> 16) | (h2 << 8) | (h2 >>> 8);
    }

    public static long kingMove(long king) {
        long attacks = shiftOne(king, W) | shiftOne(king, E);
        king |= attacks;
        attacks |= shiftOne(king, N) | shiftOne(king, S);
        return attacks;
    }

    public static long pawnAttackWhite(long pawn) {
        return shiftOne(pawn, NW) | shiftOne(pawn, NE);
    }

    public static long pawnAttackBlack(long pawn) {
        return shiftOne(pawn, SW) | shiftOne(pawn, SE);
    }

    public static void main(String[] args) {
        System.out.println(DebugUtils.debug(1L << 17, slide(1L << 17, 0, NE)));
    }
}
