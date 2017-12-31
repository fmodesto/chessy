package com.fmotech.chessy;

import static com.fmotech.chessy.BitOperations.lowestBit;
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
import static com.fmotech.chessy.MoveTables.BATT3;
import static com.fmotech.chessy.MoveTables.BATT4;
import static com.fmotech.chessy.MoveTables.RATT1;
import static com.fmotech.chessy.MoveTables.RATT2;
import static com.fmotech.chessy.Utils.BIT;
import static com.fmotech.chessy.Utils.OTHER;
import static com.fmotech.chessy.Utils.RANK;
import static com.fmotech.chessy.Utils.SYMBOLS;
import static com.fmotech.chessy.Utils.TEST;

public class MoveGenerator {

    public static void main(String[] args) {
        board = new Board();
        board.load("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        perft(WHITE, 2, false);
        for (int i = 1; i < 8; i++) {
            long time = System.currentTimeMillis();
            long perft = perft(WHITE, i, false);
            time = System.currentTimeMillis() - time;
            System.out.println(i + " " + perft + " " + time);
        }
    }

    static Board board;

    static long perft(int sideToMove, int d, boolean div) {
        if (d == 0) return 1;
        int ply = 63 - d;
        long count = 0L;
        int moveList[] = generateDirtyMoves(sideToMove, ply, board);
        for (int i = 1; i < moveList[0]; i++) {
            int move = moveList[i];
            board.doMove(move);
//            System.out.println(displayMove(move));
//            board.print();
//            System.out.flush();
//            board.assertBoard();
            if (positionAttacked(board.kingPosition(sideToMove), sideToMove, board)) {
                board.undoMove(move);
                continue;
            }
            long n = perft(OTHER(sideToMove), d - 1, false);
            count += n;
            if (div)
                System.out.println(displayMove(move) + " " + n);
            board.undoMove(move);
        }
        return count;
    }

    private static String displayMove(int move) {
        String text = (String.valueOf((char)('a' + Move.from(move) % 8))
                + String.valueOf((char)('1' + Move.from(move) / 8))
                + String.valueOf((char)('a' + Move.to(move) % 8))
                + String.valueOf((char)('1' + Move.to(move) / 8)));
        if (Move.promotion(move) != 0)
            text += String.valueOf((char)(SYMBOLS.charAt(Move.promotion(move))+32));
        return text;
    }

    private static int[] generateDirtyMoves(int sideToMove, int ply, Board board) {
        int[] moveList = board.getMoveList(ply);
        moveList[0] = 1;
        generateNonCaptureMoves(moveList, sideToMove, board);
        generateCaptureMoves(moveList, sideToMove, board, true);
        return moveList;
    }

    public static void generateNonCaptureMoves(int[] moves, int sideToMove, Board board) {
        long own = board.get(sideToMove);
        long enemy = board.get(OTHER(sideToMove));
        long pieces = own | enemy;

        long next = board.get(PAWN) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            int delta = sideToMove == WHITE ? 8 : -8;
            int to = from + delta;
            if (!TEST(to, pieces)) {
                boolean promotion = RANK(from) == (sideToMove == BLACK ? 1 : 6);
                if (!promotion)
                    moves[moves[0]++] = Move.create(from, to, PAWN, 0, 0, sideToMove);
                else {
                    moves[moves[0]++] = Move.create(from, to, PAWN, 0, QUEEN, sideToMove);
                    moves[moves[0]++] = Move.create(from, to, PAWN, 0, ROOK, sideToMove);
                    moves[moves[0]++] = Move.create(from, to, PAWN, 0, BISHOP, sideToMove);
                    moves[moves[0]++] = Move.create(from, to, PAWN, 0, KNIGHT, sideToMove);
                }
                if (RANK(from) == (sideToMove == WHITE ? 1 : 6)) {
                    to += delta;
                    if (!TEST(to, pieces))
                        moves[moves[0]++] = Move.create(from, to, PAWN, 0, 0, sideToMove);
                }
            }
            next = nextLowestBit(next);
        }

        next = board.get(KNIGHT) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long target = MoveTables.KNIGHT[from] & ~pieces;
            registerMove(moves, sideToMove, from, KNIGHT, target);
            next = nextLowestBit(next);
        }

        next = board.get(BISHOP) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long target = bishopMove(from, pieces) & ~pieces;
            registerMove(moves, sideToMove, from, BISHOP, target);
            next = nextLowestBit(next);
        }

        next = board.get(ROOK) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long target = rookMove(from, pieces) & ~pieces;
            registerMove(moves, sideToMove, from, ROOK, target);
            if (board.castle() != 0) {
                if (sideToMove == BLACK) {
                    if ((board.castle() & 128) != 0 && (from == 63) && (RATT1(63, pieces) & BIT(61)) != 0
                            && !(positionAttacked(60, sideToMove, board) | positionAttacked(61, sideToMove, board))) {
                        moves[moves[0]++] = Move.create(60, 62, KING, 0, 0, sideToMove);
                    }
                    if ((board.castle() & 512) != 0 && (from == 56) && (RATT1(56, pieces) & BIT(59)) != 0
                        && !(positionAttacked(60, sideToMove, board) | positionAttacked(59, sideToMove, board))) {
                        moves[moves[0]++] = Move.create(60, 58, KING, 0, 0, sideToMove);
                    }
                } else {
                    if ((board.castle() & 64) != 0 && (from == 7) && (RATT1(7, pieces) & BIT(5)) != 0
                            && !(positionAttacked(4, sideToMove, board) | positionAttacked(5, sideToMove, board))) {
                        moves[moves[0]++] = Move.create(4, 6, KING, 0, 0, sideToMove);
                    }
                    if ((board.castle() & 256) != 0 && (from == 0) && (RATT1(0, pieces) & BIT(3)) != 0
                            && !(positionAttacked(4, sideToMove, board) | positionAttacked(3, sideToMove, board))) {
                        moves[moves[0]++] = Move.create(4, 2, KING, 0, 0, sideToMove);
                    }
                }
            }
            next = nextLowestBit(next);
        }

        next = board.get(QUEEN) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long target = queenMove(from, pieces) & ~pieces;
            registerMove(moves, sideToMove, from, QUEEN, target);
            next = nextLowestBit(next);
        }

        next = board.get(KING) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long target = MoveTables.KING[from] & ~pieces;
            registerMove(moves, sideToMove, from, KING, target);
            next = nextLowestBit(next);
        }
    }

    public static void generateCaptureMoves(int[] moves, int sideToMove, Board board, boolean allPromotions) {
        long own = board.get(sideToMove);
        long enemy = board.get(OTHER(sideToMove));
        long pieces = own | enemy;
        long enPassant = board.enPassant(sideToMove);

        long next = board.get(PAWN) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long target = MoveTables.PAWN_ATTACK[sideToMove][from] & (enPassant | enemy);
            boolean promotion = RANK(from) == (sideToMove == BLACK ? 1 : 6);
            if (target != 0 && promotion && allPromotions) {
                registerAttackMove(moves, sideToMove, from, PAWN, target, QUEEN, board);
                registerAttackMove(moves, sideToMove, from, PAWN, target, ROOK, board);
                registerAttackMove(moves, sideToMove, from, PAWN, target, BISHOP, board);
                registerAttackMove(moves, sideToMove, from, PAWN, target, KNIGHT, board);
            } else {
                registerAttackMove(moves, sideToMove, from, PAWN, target, promotion ? QUEEN : 0, board);
            }
            next = nextLowestBit(next);
        }

        next = board.get(KNIGHT) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long target = MoveTables.KNIGHT[from] & enemy;
            registerAttackMove(moves, sideToMove, from, KNIGHT, target, 0, board);
            next = nextLowestBit(next);
        }

        next = board.get(BISHOP) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long target = bishopMove(from, pieces) & enemy;
            registerAttackMove(moves, sideToMove, from, BISHOP, target, 0, board);
            next = nextLowestBit(next);
        }

        next = board.get(ROOK) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long target = rookMove(from, pieces) & enemy;
            registerAttackMove(moves, sideToMove, from, ROOK, target, 0, board);
            next = nextLowestBit(next);
        }

        next = board.get(QUEEN) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long target = queenMove(from, pieces) & enemy;
            registerAttackMove(moves, sideToMove, from, QUEEN, target, 0, board);
            next = nextLowestBit(next);
        }

        next = board.get(KING) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long target = MoveTables.KING[from] & enemy;
            registerAttackMove(moves, sideToMove, from, KING, target, 0, board);
            next = nextLowestBit(next);
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    public static boolean positionAttacked(int position, int sideToMove, Board board) {
        long pieces = board.get(WHITE) | board.get(BLACK);
        long enemy = board.get(OTHER(sideToMove));
        if ((MoveTables.PAWN_ATTACK[sideToMove][position] & enemy & board.get(PAWN)) != 0) return true;
        if ((MoveTables.KNIGHT[position] & enemy & board.get(KNIGHT)) != 0) return true;
        if ((MoveTables.KING[position] & enemy & board.get(KING)) != 0) return true;
        if ((RATT1(position, pieces) & enemy & (board.get(ROOK) | board.get(QUEEN))) != 0) return true;
        if ((RATT2(position, pieces) & enemy & (board.get(ROOK) | board.get(QUEEN))) != 0) return true;
        if ((BATT3(position, pieces) & enemy & (board.get(BISHOP) | board.get(QUEEN))) != 0) return true;
        if ((BATT4(position, pieces) & enemy & (board.get(BISHOP) | board.get(QUEEN))) != 0) return true;
        return false;
    }

    private static void registerMove(int[] moves, int sideToMove, int from, int piece, long target) {
        while (target != 0) {
            int to = lowestBitPosition(target);
            moves[moves[0]++] = Move.create(from, to, piece, 0, 0, sideToMove);
            target = nextLowestBit(target);
        }
    }

    private static void registerAttackMove(int[] moves, int sideToMove, int from, int piece, long target, int promotion, Board board) {
        while (target != 0) {
            int to = lowestBitPosition(target);
            long capture = lowestBit(target);
            moves[moves[0]++] = Move.create(from, to, piece, board.pieceType(capture), promotion, sideToMove);
            target = nextLowestBit(target);
        }
    }

    private static long rookMove(int pos, long pieces) {
        return (RATT1(pos, pieces) | RATT2(pos, pieces));
    }

    private static long bishopMove(int pos, long pieces) {
        return (BATT3(pos, pieces) | BATT4(pos, pieces));
    }

    private static long queenMove(int pos, long pieces) {
        return (RATT1(pos, pieces) | RATT2(pos, pieces) | BATT3(pos, pieces) | BATT4(pos, pieces));
    }
}
