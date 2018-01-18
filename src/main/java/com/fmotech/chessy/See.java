package com.fmotech.chessy;

import static com.fmotech.chessy.BitOperations.lowestBit;
import static com.fmotech.chessy.Board.BISHOP;
import static com.fmotech.chessy.Board.BLACK;
import static com.fmotech.chessy.Board.EN_PASSANT;
import static com.fmotech.chessy.Board.KING;
import static com.fmotech.chessy.Board.KNIGHT;
import static com.fmotech.chessy.Board.PAWN;
import static com.fmotech.chessy.Board.QUEEN;
import static com.fmotech.chessy.Board.ROOK;
import static com.fmotech.chessy.Board.WHITE;
import static com.fmotech.chessy.BoardUtils.BIT;
import static com.fmotech.chessy.BoardUtils.OTHER;
import static com.fmotech.chessy.MagicBitboard.bishopMove;
import static com.fmotech.chessy.MagicBitboard.rookMove;

public class See {

    private static final int[] MATERIAL = { 0, 100, 100, 290, 310, 500, 950, 16000 };

    private int[] gain = new int[32];

    public int evaluate(Board board, int move) {
        int from = Move.from(move);
        int to = Move.to(move);
        int side = Move.sideToMove(move);

        int pieceValue = MATERIAL[Move.capture(move)];
        int piece = Move.piece(move);

        long pieces = (board.get(WHITE) | board.get(Board.BLACK)) ^ BIT(from);
        if (Move.capture(move) == EN_PASSANT) pieces ^= board.enPassantPawn(side);

        gain[0] = pieceValue;
        pieceValue = MATERIAL[piece];

        long rookMask = rookMove(to, (long) 0);
        long rookQueen = board.get(ROOK) | board.get(QUEEN);
        long bishopQueen = board.get(BISHOP) | board.get(QUEEN);

        long attacks = MagicBitboard.pawnAttack(to, BLACK) & board.get(PAWN) & board.get(WHITE)
                | MagicBitboard.pawnAttack(to,WHITE) & board.get(PAWN) & board.get(BLACK)
                | MagicBitboard.knightMove(to) & board.get(KNIGHT)
                | MagicBitboard.kingMove(to) & board.get(KING)
                | rookMove(to, pieces) & rookQueen
                | bishopMove(to, pieces) & bishopQueen;

        int depth = 1;
        side = OTHER(side);

        while ((attacks & board.get(side)) != 0) {
            long bit;
            if ((bit = board.get(PAWN) & board.get(side) & attacks) != 0) piece = PAWN;
            else if ((bit = board.get(KNIGHT) & board.get(side) & attacks) != 0) piece = KNIGHT;
            else if ((bit = board.get(BISHOP) & board.get(side) & attacks) != 0) piece = BISHOP;
            else if ((bit = board.get(ROOK) & board.get(side) & attacks) != 0) piece = ROOK;
            else if ((bit = board.get(QUEEN) & board.get(side) & attacks) != 0) piece = QUEEN;
            else if ((bit = board.get(KING) & board.get(side) & attacks) != 0) piece = KING;
            else break;

            bit = lowestBit(bit);
            pieces ^= bit;
            if (piece != KNIGHT)
                attacks |= (bit & rookMask) != 0 ? rookMove(to, pieces) & rookQueen : bishopMove(to, pieces) & bishopQueen;
            attacks &= pieces;

            gain[depth] = -gain[depth - 1] + pieceValue;
            pieceValue = MATERIAL[piece];

            depth++;
            side = OTHER(side);
        }

        while (--depth > 0)
            gain[depth - 1] = -Math.max(-gain[depth - 1], gain[depth]);

        return gain[0];
    }
}
