package com.fmotech.chessy;

public class Move {

    private static final int SQUARE_MASK = 0x3F;
    private static final int PIECE_MASK = 0x07;
    private static final int SIDE_MASK = 0x01;

    public static int create(int from, int to, int piece, int capture, int promotion, int sideToMove) {
        return (from & SQUARE_MASK)
                | (to & SQUARE_MASK) << 6
                | (piece & PIECE_MASK) << 15
                | (capture & PIECE_MASK) << 19
                | (promotion & PIECE_MASK) << 12
                | (sideToMove & SIDE_MASK) << 18;
    }

    public static int from(int move) {
        return move & SQUARE_MASK;
    }

    public static int to(int move) {
        return (move >>> 6) & SQUARE_MASK;
    }

    public static int piece(int move) {
        return (move >>> 15) & PIECE_MASK;
    }

    public static int capture(int move) {
        return (move >>> 19) & PIECE_MASK;
    }

    public static int promotion(int move) {
        return (move >>> 12) & PIECE_MASK;
    }

    public static int sideToMove(int move) {
        return (move >>> 18) & SIDE_MASK;
    }
}
