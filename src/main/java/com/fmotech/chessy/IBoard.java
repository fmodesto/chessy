package com.fmotech.chessy;

public interface IBoard {
    long get(int bitBoard);

    int kingPosition(int sideToMove);

    int pieceType(long bit);

    int castle();

    int[] getMoveList(int ply);

    int enPassantPosition();

    long enPassant(int sideToMove);

    long enPassantPawn(int sideToMove);

    default void print() {
        System.out.println(DebugUtils.debug(get(0), get(1), get(2), get(3), get(4), get(5), get(6), get(7)));
    }

}
