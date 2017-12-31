package com.fmotech.chessy;

import static com.fmotech.chessy.Board.BLACK;

public class Utils {
    public static final String SYMBOLS = "··PNBRQK";

    public static long BIT(int i) { return 1L << i; }
    public static int OTHER(int i) { return i ^ BLACK; }
    public static boolean TEST(int position, long bitBoard) { return (BIT(position) & bitBoard) != 0; }
    public static int RANK(int position) { return position >>> 3; }
    public static int FILE(int position) { return position & 0x07; }
}
