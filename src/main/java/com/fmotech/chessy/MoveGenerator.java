package com.fmotech.chessy;

import java.util.HashMap;
import java.util.Map;

import static com.fmotech.chessy.BitOperations.lowestBitPosition;
import static com.fmotech.chessy.BitOperations.nextLowestBit;
import static com.fmotech.chessy.Board.BISHOP;
import static com.fmotech.chessy.Board.BLACK;
import static com.fmotech.chessy.Board.KING;
import static com.fmotech.chessy.Board.KNIGHT;
import static com.fmotech.chessy.Board.PAWN;
import static com.fmotech.chessy.Board.QUEEN;
import static com.fmotech.chessy.Board.ROOK;
import static com.fmotech.chessy.Board.WHITE;
import static com.fmotech.chessy.BoardUtils.BIT;
import static com.fmotech.chessy.BoardUtils.OTHER;
import static com.fmotech.chessy.BoardUtils.RANK;
import static com.fmotech.chessy.BoardUtils.TEST;
import static com.fmotech.chessy.MagicBitboard.MASK;
import static com.fmotech.chessy.MagicBitboard.SEGMENT;
import static com.fmotech.chessy.MagicBitboard.bishopMove;
import static com.fmotech.chessy.MagicBitboard.bishopXRay;
import static com.fmotech.chessy.MagicBitboard.kingMove;
import static com.fmotech.chessy.MagicBitboard.knightMove;
import static com.fmotech.chessy.MagicBitboard.pawnAttack;
import static com.fmotech.chessy.MagicBitboard.pawnDoubleMove;
import static com.fmotech.chessy.MagicBitboard.pawnMove;
import static com.fmotech.chessy.MagicBitboard.queenMove;
import static com.fmotech.chessy.MagicBitboard.rookMove;
import static com.fmotech.chessy.MagicBitboard.rookXRay;

public class MoveGenerator {

    private static final long[] PAWN_START = { 0x000000000000ff00L, 0x00ff000000000000L };
    private static final long[] PAWN_PROMOTE = { 0x00ff000000000000L, 0x000000000000ff00L };
    private static final long[] PAWN_EN_PASSANT = { 0x000000ff00000000L, 0x00000000ff000000L};

    public static long countMoves(int depth, Board board, boolean div) {
        return perft(board, depth, div);
    }

    public static Map<String, Long> moves = new HashMap<>();
    static long perft(Board board, int depth, boolean div) {
        int side = board.sideToMove();
        long count = 0L;
        long check = attackingPieces(board.kingPosition(side), side, board);
        long pin = pinnedPieces(board.kingPosition(side), side, board);
        int moveList[] = generate(board, depth, check, pin);
        if (depth == 1)
            return (moveList[0] - 1) + (254 - moveList[255]);
        if (div) moves.clear();

        for (int i = 1; i < moveList[0]; i++) {
            int move = moveList[i];
            board.doMove(move);
            long n = perft(board, depth - 1, false);
            count += n;
            if (div) {
                System.out.println(Formatter.moveToFen(move) + " " + n + " " + move);
                moves.put(Formatter.moveToFen(move), n);
            }
            board.undoMove(move);
        }
        for (int i = 254; i > moveList[255]; i--) {
            int move = moveList[i];
            board.doMove(move);
            long n = perft(board, depth - 1, false);
            count += n;
            if (div) {
                System.out.println(Formatter.moveToFen(move) + " " + n + " " + move);
                moves.put(Formatter.moveToFen(move), n);
            }
            board.undoMove(move);
        }
        return count;
    }

    public static int[] generate(Board board, int ply, long check, long pin) {
        int side = board.sideToMove();
        int[] moves = board.getMoveList(ply);
        moves[0] = 1;
        moves[255] = 254;

        long own = board.get(side);
        long enemy = board.get(OTHER(side));
        long pieces = own | enemy;
        int king = board.kingPosition(side);

        long next = board.get(KING) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long target = kingMove(from);
            registerMoves(moves, side, from, KING, target & ~pieces, false, true, board);
            registerAttackMoves(moves, side, from, KING, target & enemy, false, true, board);
            next = nextLowestBit(next);
        }

        if (check != 0 && nextLowestBit(check) != 0) {
            // Can't intercept or capture more than one piece
            return moves;
        }

        long checkMask = check != 0 ? check | SEGMENT(king, lowestBitPosition(check), pieces) : -1;
        boolean enPassantCheck = check != 0 && check == board.enPassantPawn(side);

        next = board.get(PAWN) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long mask = pinMask(king, from, pin) & (enPassantCheck ? checkMask | board.enPassant(side) : checkMask);
            long target = pawnMove(from, side) & ~pieces;
            if (TEST(from, PAWN_START[side]) && target != 0) {
                target |= pawnDoubleMove(from, side) & ~pieces;
            }
            boolean promotion = TEST(from, PAWN_PROMOTE[side]);
            registerMoves(moves, side, from, PAWN, target & mask, promotion, false, board);
            target = pawnAttack(from, side) & (enemy | board.enPassant(side)) & mask;
            if ((target & board.enPassant(side)) != 0 && RANK(king) == RANK(from)) {
                long ray = rookMove(from, pieces ^ board.enPassantPawn(side)) & PAWN_EN_PASSANT[side];
                if (TEST(king, ray) && (ray & (board.get(ROOK) | board.get(QUEEN)) & enemy) != 0) {
                    target ^= board.enPassant(side);
                }
            }
            registerAttackMoves(moves, side, from, PAWN, target, promotion, false, board);
            next = nextLowestBit(next);
        }

        next = board.get(KNIGHT) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long mask = pinMask(king, from, pin) & checkMask;
            long target = knightMove(from) & mask;
            registerMoves(moves, side, from, KNIGHT, target & ~pieces, false, false, board);
            registerAttackMoves(moves, side, from, KNIGHT, target & enemy, false, false, board);
            next = nextLowestBit(next);
        }

        next = board.get(BISHOP) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long mask = pinMask(king, from, pin) & checkMask;
            long target = bishopMove(from, pieces) & mask;
            registerMoves(moves, side, from, BISHOP, target & ~pieces, false, false, board);
            registerAttackMoves(moves, side, from, BISHOP, target & enemy, false, false, board);
            next = nextLowestBit(next);
        }

        next = board.get(ROOK) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long mask = pinMask(king, from, pin) & checkMask;
            long target = rookMove(from, pieces) & mask;
            registerMoves(moves, side, from, ROOK, target & ~pieces, false, false, board);
            registerAttackMoves(moves, side, from, ROOK, target & enemy, false, false, board);
            if (check == 0 && TEST(from, board.castle(side)) && TEST(king, target)) {
                int delta = from > king ? 1 : -1;
                if (!positionAttacked(king + delta, side, board) && !positionAttacked(king + delta + delta, side, board)) {
                    registerMoves(moves, side, king, KING, BIT(king + delta + delta), false, false, board);
                }
            }
            next = nextLowestBit(next);
        }

        next = board.get(QUEEN) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long mask = pinMask(king, from, pin) & checkMask;
            long target = queenMove(from, pieces) & mask;
            registerMoves(moves, side, from, QUEEN, target & ~pieces, false, false, board);
            registerAttackMoves(moves, side, from, QUEEN, target & enemy, false, false, board);
            next = nextLowestBit(next);
        }

        return moves;
    }

    private static long pinMask(int king, int from, long pin) {
        return TEST(from, pin) ? MASK(from, king) : -1;
    }

    private static void registerMoves(int[] moves, int sideToMove, int from, int piece, long target, boolean promotion, boolean validate, Board board) {
        while (target != 0) {
            int to = lowestBitPosition(target);
            target = nextLowestBit(target);
            if (validate && positionAttacked(to, sideToMove, board))
                continue;
            if (promotion) {
                moves[moves[0]++] = Move.create(from, to, piece, 0, QUEEN, sideToMove);
                moves[moves[255]--] = Move.create(from, to, piece, 0, ROOK, sideToMove);
                moves[moves[255]--] = Move.create(from, to, piece, 0, BISHOP, sideToMove);
                moves[moves[255]--] = Move.create(from, to, piece, 0, KNIGHT, sideToMove);
            } else {
                moves[moves[255]--] = Move.create(from, to, piece, 0, 0, sideToMove);
            }
        }
    }

    private static void registerAttackMoves(int[] moves, int sideToMove, int from, int piece, long target, boolean promotion, boolean validate, Board board) {
        while (target != 0) {
            int to = lowestBitPosition(target);
            target = nextLowestBit(target);
            if (validate && positionAttacked(to, sideToMove, board))
                continue;
            long capture = BIT(to);
            if (promotion) {
                moves[moves[0]++] = Move.create(from, to, piece, board.pieceType(capture), QUEEN, sideToMove);
                moves[moves[255]--] = Move.create(from, to, piece, board.pieceType(capture), ROOK, sideToMove);
                moves[moves[255]--] = Move.create(from, to, piece, board.pieceType(capture), BISHOP, sideToMove);
                moves[moves[255]--] = Move.create(from, to, piece, board.pieceType(capture), KNIGHT, sideToMove);
            } else {
                moves[moves[0]++] = Move.create(from, to, piece, board.pieceType(capture), 0, sideToMove);
            }
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    public static boolean positionAttacked(int position, int sideToMove, Board board) {
        long pieces = (board.get(WHITE) | board.get(BLACK)) ^ BIT(board.kingPosition(sideToMove));
        long enemy = board.get(OTHER(sideToMove));
        if ((pawnAttack(position, sideToMove) & enemy & board.get(PAWN)) != 0) return true;
        if ((knightMove(position) & enemy & board.get(KNIGHT)) != 0) return true;
        if ((kingMove(position) & enemy & board.get(KING)) != 0) return true;
        if ((rookMove(position, pieces) & enemy & (board.get(ROOK) | board.get(QUEEN))) != 0) return true;
        if ((bishopMove(position, pieces) & enemy & (board.get(BISHOP) | board.get(QUEEN))) != 0) return true;
        return false;
    }

    public static long attackingPieces(int position, int sideToMove, Board board) {
        long enemy = board.get(OTHER(sideToMove));
        long pieces = board.get(sideToMove) | enemy;
        return pawnAttack(position, sideToMove) & board.get(PAWN) & enemy
                | knightMove(position) & board.get(KNIGHT) & enemy
                | rookMove(position, pieces) & (board.get(ROOK) | board.get(QUEEN)) & enemy
                | bishopMove(position, pieces) & (board.get(BISHOP) | board.get(QUEEN)) & enemy;
    }

    public static long pinnedPieces(int position, int sideToMove, Board board) {
        long own = board.get(sideToMove);
        long enemy = board.get(OTHER(sideToMove));
        long pieces = own | enemy;
        long pin = 0L;
        long rookQueen = board.get(ROOK) | board.get(QUEEN);
        long bishopQueen = board.get(BISHOP) | board.get(QUEEN);

        long next = rookXRay(position, pieces) & rookQueen & enemy;
        while (next != 0) {
            int pinner = lowestBitPosition(next);
            pin |= rookMove(pinner, pieces) & rookMove(position, pieces) & own;
            next = nextLowestBit(next);
        }
        next = bishopXRay(position, pieces) & bishopQueen & enemy;
        while (next != 0) {
            int pinner = lowestBitPosition(next);
            pin |= bishopMove(pinner, pieces) & bishopMove(position, pieces) & own;
            next = nextLowestBit(next);
        }
        return pin;
    }
}
