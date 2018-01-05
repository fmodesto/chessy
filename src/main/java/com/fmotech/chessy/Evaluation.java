package com.fmotech.chessy;

import static com.fmotech.chessy.BitOperations.bitCount;
import static com.fmotech.chessy.BitOperations.highInt;
import static com.fmotech.chessy.BitOperations.joinInts;
import static com.fmotech.chessy.BitOperations.lowInt;
import static com.fmotech.chessy.BitOperations.lowestBit;
import static com.fmotech.chessy.BitOperations.lowestBitPosition;
import static com.fmotech.chessy.BitOperations.nextLowestBit;
import static com.fmotech.chessy.BitOperations.northFill;
import static com.fmotech.chessy.BitOperations.southFill;
import static com.fmotech.chessy.BitOperations.sparseBitCount;
import static com.fmotech.chessy.Board.BISHOP;
import static com.fmotech.chessy.Board.BLACK;
import static com.fmotech.chessy.Board.KNIGHT;
import static com.fmotech.chessy.Board.PAWN;
import static com.fmotech.chessy.Board.QUEEN;
import static com.fmotech.chessy.Board.ROOK;
import static com.fmotech.chessy.Board.WHITE;
import static com.fmotech.chessy.KoggeStone.E;
import static com.fmotech.chessy.KoggeStone.N;
import static com.fmotech.chessy.KoggeStone.NE;
import static com.fmotech.chessy.KoggeStone.NW;
import static com.fmotech.chessy.KoggeStone.S;
import static com.fmotech.chessy.KoggeStone.SE;
import static com.fmotech.chessy.KoggeStone.SW;
import static com.fmotech.chessy.KoggeStone.W;
import static com.fmotech.chessy.KoggeStone.shiftOne;
import static com.fmotech.chessy.MoveGenerator.pinnedPieces;
import static com.fmotech.chessy.MoveTables.DIR;
import static com.fmotech.chessy.MoveTables.MASK;
import static com.fmotech.chessy.Utils.BIT;
import static com.fmotech.chessy.Utils.RANK;
import static com.fmotech.chessy.Utils.TEST;
import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;

public class Evaluation {

    private static final int[] KNIGHT_MOBILITY = range(0, 64).map(i -> (bitCount(MoveTables.KNIGHT[i]) - 1) * 6).toArray();
    private static final int[] KING_MOBILITY = range(0, 64).map(i -> (bitCount(MoveTables.KNIGHT[i]) / 2) * 2).toArray();

    private static final long[][] PAWN_FREE = new long[][] {
            range(0, 64).mapToLong(Utils::BIT).map(b -> northFill(shiftOne(b, NW) | shiftOne(b, N) | shiftOne(b, NE))).toArray(),
            range(0, 64).mapToLong(Utils::BIT).map(b -> southFill(shiftOne(b, SW) | shiftOne(b, S) | shiftOne(b, SE))).toArray() };
    private static final long[][] PAWN_FILE = new long[][] {
            range(0, 64).mapToLong(Utils::BIT).map(b -> northFill(shiftOne(b, N))).toArray(),
            range(0, 64).mapToLong(Utils::BIT).map(b -> southFill(shiftOne(b, S))).toArray() };
    private static final long[][] PAWN_HELP = new long[][] {
            range(0, 64).mapToLong(Utils::BIT).map(b -> shiftOne(b, W) | shiftOne(b, SW) | shiftOne(b, SE) | shiftOne(b, E)).toArray(),
            range(0, 64).mapToLong(Utils::BIT).map(b -> shiftOne(b, W) | shiftOne(b, NW) | shiftOne(b, NE) | shiftOne(b, E)).toArray() };
    private static final int[][] PAWN_RUN = new int[][] {
            range(0, 64).map(i -> asList(0, 0, 1, 8, 16, 32, 64, 128).get(RANK(i))).toArray(),
            range(0, 64).map(i -> asList(0, 0, 1, 8, 16, 32, 64, 128).get(7 - RANK(i))).toArray() };

    public int evaluate(Board board, int sideToMove) {
        long ev0 = evaluateSide(WHITE, BLACK, board);
        long ev1 = evaluateSide(BLACK, WHITE, board);

//        if (highInt(ev1) < 6) ev0 += KING_MOBILITY[board.kingPosition(WHITE)] * (6 - highInt(ev1));
//        if (highInt(ev0) < 6) ev1 += KING_MOBILITY[board.kingPosition(BLACK)] * (6 - highInt(ev0));

        return sideToMove != 0 ? lowInt(ev1) - lowInt(ev0) : lowInt(ev0) - lowInt(ev1);
    }

    static long evaluateSide(int side, int otherSide, Board board) {
        int pieceValue = 0;
        int mobility = 0;
        int kingAttack = 0;

        int king = board.kingPosition(side);
        long kingSurrounds = MoveTables.KING[board.kingPosition(otherSide)];
        long pin = pinnedPieces(king, otherSide, board);
        
        long own = board.get(side);
        long enemy = board.get(otherSide);
        long pieces = own | enemy;

        long next = board.get(PAWN) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            int ppos = PAWN_RUN[side][from];
            long m = BIT(from + (side == BLACK ? -8 : 8)) & ~pieces;
            long attack = MoveTables.PAWN_ATTACK[side][from];
            if ((attack & kingSurrounds) != 0) kingAttack += sparseBitCount(attack & kingSurrounds) << 4;
            if (TEST(from, pin)) {
                if (DIR(from, board.kingPosition(side)) != 2) m = 0;
            } else {
                ppos += sparseBitCount(attack & board.get(PAWN) & own) << 2;
            }
            if (m != 0) ppos += 8; else ppos -= 8;
            if ((PAWN_FILE[side][from] & board.get(PAWN) & enemy) == 0) { //Free file?
                if ((PAWN_FREE[side][from] & board.get(PAWN) & enemy) == 0) ppos *= 2; //Free run?
                if ((PAWN_HELP[side][from] & board.get(PAWN) & own) == 0) ppos -= 33; //Hanging backpawn?
            }
            mobility += ppos;
            next = nextLowestBit(next);
        }

//        next = board.get(KNIGHT) & own;
//        while (next != 0) {
//            pieceValue += 1;
//            int from = lowestBitPosition(next);
//            long attack = MoveTables.KNIGHT[from];
//            if ((attack & kingSurrounds) != 0) kingAttack += sparseBitCount(attack & kingSurrounds) << 4;
//            if (!TEST(from, pin)) mobility += KNIGHT_MOBILITY[from];
//            next = nextLowestBit(next);
//        }
//
//        pieces ^= BIT(board.kingPosition(otherSide)); //Opposite King doesn't block mobility at all
//        next = board.get(QUEEN) & own;
//        while (next != 0) {
//            pieceValue += 4;
//            int from = lowestBitPosition(next);
//            long mask = (pin & lowestBit(next)) != 0 ? MASK(from, king) : -1;
//            long attack = MoveGenerator.queenMove(from, pieces) & mask;
//            if ((attack & kingSurrounds) != 0) kingAttack += sparseBitCount(attack & kingSurrounds) << 4;
//            if (!TEST(from, pin)) mobility += bitCount(attack);
//            next = nextLowestBit(next);
//        }
//
//        pieces ^= (board.get(ROOK) | board.get(QUEEN)) & enemy; //Opposite Queen & Rook doesn't block mobility for bishop
//        next = board.get(BISHOP) & own;
//        while (next != 0) {
//            pieceValue += 1;
//            int from = lowestBitPosition(next);
//            long mask = (pin & lowestBit(next)) != 0 ? MASK(from, king) : -1;
//            long attack = MoveGenerator.bishopMove(from, pieces) & mask;
//            if ((attack & kingSurrounds) != 0) kingAttack += sparseBitCount(attack & kingSurrounds) << 4;
//            mobility += bitCount(attack) << (mask == -1 ? 0 : 3);
//            next = nextLowestBit(next);
//        }
//
//        pieces ^= board.get(ROOK) & enemy; //Opposite Queen doesn't block mobility for rook.
//        pieces ^= board.get(ROOK) & own & ~pin; //Own non-pinned Rook doesn't block mobility for rook.
//        next = board.get(ROOK) & own;
//        while (next != 0) {
//            pieceValue += 2;
//            int from = lowestBitPosition(next);
//            long mask = (pin & lowestBit(next)) != 0 ? MASK(from, king) : -1;
//            long attack = MoveGenerator.rookMove(from, pieces) & mask;
//            if ((attack & kingSurrounds) != 0) kingAttack += sparseBitCount(attack & kingSurrounds) << 4;
//            mobility += bitCount(attack) << (mask == -1 ? 0 : 2);
//            next = nextLowestBit(next);
//        }
//
//        if (pieceValue == 1 && (board.get(PAWN) & own) == 0) mobility = -200; //No mating material
//        if (pieceValue < 7) kingAttack = kingAttack * pieceValue / 7; //Reduce the bonus for attacking king squares
//        if (pieceValue < 2) pieceValue = 2;
        return joinInts(pieceValue, mobility + kingAttack);
    }
}