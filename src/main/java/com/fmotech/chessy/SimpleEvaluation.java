//package com.fmotech.chessy;
//
//import com.fmotech.chess.BitOperations;
//import com.fmotech.chess.Board;
//
//import static com.fmotech.chess.BitOperations.lowestBitPosition;
//import static com.fmotech.chess.BitOperations.nextLowestBit;
//import static com.fmotech.chess.ai.EvaluationUtils.ENEMY_SIDE;
//import static com.fmotech.chess.ai.EvaluationUtils.OWN_SIDE;
//import static com.fmotech.chess.ai.EvaluationUtils.toBoardPosition;
//
//public class SimpleEvaluation implements Evaluation {
//
//    private static final byte[][] PAWNS_TABLE = toBoardPosition(
//            0,  0,  0,  0,  0,  0,  0,  0,
//            50, 50, 50, 50, 50, 50, 50, 50,
//            10, 10, 20, 30, 30, 20, 10, 10,
//            5,  5, 10, 25, 25, 10,  5,  5,
//            0,  0,  0, 20, 20,  0,  0,  0,
//            5, -5,-10,  0,  0,-10, -5,  5,
//            5, 10, 10,-20,-20, 10, 10,  5,
//            0,  0,  0,  0,  0,  0,  0,  0);
//
//    private static final byte[][] KNIGHTS_TABLE = toBoardPosition(
//            -50,-40,-30,-30,-30,-30,-40,-50,
//            -40,-20,  0,  0,  0,  0,-20,-40,
//            -30,  0, 10, 15, 15, 10,  0,-30,
//            -30,  5, 15, 20, 20, 15,  5,-30,
//            -30,  0, 15, 20, 20, 15,  0,-30,
//            -30,  5, 10, 15, 15, 10,  5,-30,
//            -40,-20,  0,  5,  5,  0,-20,-40,
//            -50,-40,-30,-30,-30,-30,-40,-50);
//
//    private static final byte[][] BISHOPS_TABLE = toBoardPosition(
//            -20,-10,-10,-10,-10,-10,-10,-20,
//            -10,  0,  0,  0,  0,  0,  0,-10,
//            -10,  0,  5, 10, 10,  5,  0,-10,
//            -10,  5,  5, 10, 10,  5,  5,-10,
//            -10,  0, 10, 10, 10, 10,  0,-10,
//            -10, 10, 10, 10, 10, 10, 10,-10,
//            -10,  5,  0,  0,  0,  0,  5,-10,
//            -20,-10,-10,-10,-10,-10,-10,-20);
//
//    private static final byte[][] ROOKS_TABLE = toBoardPosition(
//            0,  0,  0,  0,  0,  0,  0,  0,
//            5, 10, 10, 10, 10, 10, 10,  5,
//            -5,  0,  0,  0,  0,  0,  0, -5,
//            -5,  0,  0,  0,  0,  0,  0, -5,
//            -5,  0,  0,  0,  0,  0,  0, -5,
//            -5,  0,  0,  0,  0,  0,  0, -5,
//            -5,  0,  0,  0,  0,  0,  0, -5,
//            0,  0,  0,  5,  5,  0,  0,  0);
//
//    private static final byte[][] QUEENS_TABLE = toBoardPosition(
//            -20,-10,-10, -5, -5,-10,-10,-20,
//            -10,  0,  0,  0,  0,  0,  0,-10,
//            -10,  0,  5,  5,  5,  5,  0,-10,
//            -5,  0,  5,  5,  5,  5,  0, -5,
//            0,  0,  5,  5,  5,  5,  0, -5,
//            -10,  5,  5,  5,  5,  5,  0,-10,
//            -10,  0,  5,  0,  0,  0,  0,-10,
//            -20,-10,-10, -5, -5,-10,-10,-20);
//
//    private static final byte[][] KING_MG_TABLE = toBoardPosition(
//            -30,-40,-40,-50,-50,-40,-40,-30,
//            -30,-40,-40,-50,-50,-40,-40,-30,
//            -30,-40,-40,-50,-50,-40,-40,-30,
//            -30,-40,-40,-50,-50,-40,-40,-30,
//            -20,-30,-30,-40,-40,-30,-30,-20,
//            -10,-20,-20,-20,-20,-20,-20,-10,
//            20, 20,  0,  0,  0,  0, 20, 20,
//            20, 30, 10,  0,  0, 10, 30, 20);
//
//    private static final byte[][] KING_EG_TABLE = toBoardPosition(
//            -50,-40,-30,-20,-20,-30,-40,-50,
//            -30,-20,-10,  0,  0,-10,-20,-30,
//            -30,-10, 20, 30, 30, 20,-10,-30,
//            -30,-10, 30, 40, 40, 30,-10,-30,
//            -30,-10, 30, 40, 40, 30,-10,-30,
//            -30,-10, 20, 30, 30, 20,-10,-30,
//            -30,-30,  0,  0,  0,  0,-30,-30,
//            -50,-30,-30,-30,-30,-30,-30,-50);
//
//    private static final int QUEEN = 900;
//    private static final int ROOK = 500;
//    private static final int BISHOP = 330;
//    private static final int KNIGHT = 320;
//    private static final int PAWN = 100;
//
//    @Override
//    public int evaluateBoardPosition(Board board, int alpha, int beta) {
//        int score = 0;
//        boolean endGame = board.queens() == 0
//                || endGame(board.ownQueens(), board.ownPieces(), board.ownKnights() | board.ownBishops())
//                || endGame(board.enemyQueens(), board.enemyPieces(), board.enemyKnights() | board.enemyBishops());
//
//        score += evaluate(board.ownPawns(), PAWN, PAWNS_TABLE[OWN_SIDE]);
//        score += evaluate(board.ownKnights(), KNIGHT, KNIGHTS_TABLE[OWN_SIDE]);
//        score += evaluate(board.ownBishops(), BISHOP, BISHOPS_TABLE[OWN_SIDE]);
//        score += evaluate(board.ownRooks(), ROOK, ROOKS_TABLE[OWN_SIDE]);
//        score += evaluate(board.ownQueens(), QUEEN, QUEENS_TABLE[OWN_SIDE]);
//        score += evaluate(board.ownKing(), 0, endGame ? KING_EG_TABLE[OWN_SIDE] : KING_MG_TABLE[OWN_SIDE]);
//
//        score -= evaluate(board.enemyPawns(), PAWN, PAWNS_TABLE[ENEMY_SIDE]);
//        score -= evaluate(board.enemyKnights(), KNIGHT, KNIGHTS_TABLE[ENEMY_SIDE]);
//        score -= evaluate(board.enemyBishops(), BISHOP, BISHOPS_TABLE[ENEMY_SIDE]);
//        score -= evaluate(board.enemyRooks(), ROOK, ROOKS_TABLE[ENEMY_SIDE]);
//        score -= evaluate(board.enemyQueens(), QUEEN, QUEENS_TABLE[ENEMY_SIDE]);
//        score -= evaluate(board.enemyKing(), 0, endGame ? KING_EG_TABLE[ENEMY_SIDE] : KING_MG_TABLE[ENEMY_SIDE]);
//
//        return score;
//    }
//
//    private int evaluate(long pieces, int value, byte[] table) {
//        int sum = 0;
//        while (pieces != 0) {
//            sum += value + table[lowestBitPosition(pieces)];
//            pieces = nextLowestBit(pieces);
//        }
//        return sum;
//    }
//
//    private boolean endGame(long queens, long pieces, long minorPieces) {
//        return queens == 0 || (onlyOne(queens) && onlyOne(minorPieces) && onlyOne(pieces ^ (queens | minorPieces)));
//    }
//
//    private boolean onlyOne(long queens) {
//        return BitOperations.nextLowestBit(queens) == 0;
//    }
//}
