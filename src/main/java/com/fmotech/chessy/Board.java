package com.fmotech.chessy;

import java.util.Arrays;
import java.util.Random;

import static com.fmotech.chessy.BitOperations.highInt;
import static com.fmotech.chessy.BitOperations.joinInts;
import static com.fmotech.chessy.BitOperations.lowInt;
import static com.fmotech.chessy.Utils.BIT;
import static com.fmotech.chessy.Utils.OTHER;
import static java.lang.Character.isSpaceChar;
import static java.lang.Character.toUpperCase;

public class Board {
    private static final int CASTLE_MASK = 0xffffffc0;
    private static final int EN_PASSANT_MASK = 0x0000003f;
    private static final int INCREMENT_FIFTY_PLY = 0x401;
    private static final int FIFTY_MASK = 0x3ff;
    private static final int CASTLING_MASK = 0x140;

    public static final int WHITE = 0;
    public static final int BLACK = 1;
    public static final int PAWN = 2;
    public static final int KNIGHT = 3;
    public static final int BISHOP = 4;
    public static final int ROOK = 5;
    public static final int QUEEN = 6;
    public static final int KING = 7;

    public static final int EN_PASSANT = 1;

    private static final int[] MATERIAL = { 0, 0, 100, 290, 310, 500, 950, 0 };
    public static final int[] CASTLE_REVOKE = createCastleRevoke();
    private static final int[] CASTLE_ROOK = createCastleRook();
    private static final long[] ZOBRIST = createZobrist();

    public static final Board INIT = Board.load("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

    private long[] bitBoards = new long[8];
    private long[] moveStack = new long[0x400];
    private int[] material = new int[2];
    private int[] kings = new int[2];
    private int[][] moveList = new int[64][256];

    private int flags = 0;
    private int counter = 0;
    private long hash = 0;
    private int sideToMove = WHITE;

    private Board() {}

    public int pieceType(long bit) {
        if ((bit & bitBoards[PAWN]) != 0) return PAWN;
        if ((bit & bitBoards[KNIGHT]) != 0) return KNIGHT;
        if ((bit & bitBoards[BISHOP]) != 0) return BISHOP;
        if ((bit & bitBoards[ROOK]) != 0) return ROOK;
        if ((bit & bitBoards[QUEEN]) != 0) return QUEEN;
        if ((bit & bitBoards[KING]) != 0) return KING;
        return EN_PASSANT;
    }

    public long get(int bitBoard) {
        return bitBoards[bitBoard];
    }

    public int fifty() {
        return (counter >>> 10) & 0x3FF;
    }

    public int ply() {
        return counter & 0x3FF;
    }

    public void doMove(int move) {
        moveStack[ply()] = joinInts(counter, flags);
        move(move);
    }

    public void undoMove(int move) {
        long data = moveStack[ply() - 1];
        move(move);
        counter = highInt(data);
        flags = lowInt(data);
    }

    private void move(int move) {
        int from = Move.from(move);
        int to = Move.to(move);
        int piece = Move.piece(move);
        int capture = Move.capture(move);
        int sideToMove = Move.sideToMove(move);

        bitBoards[sideToMove] ^= BIT(from) | BIT(to);
        bitBoards[piece] ^= BIT(from) | BIT(to);
        hash ^= zobrist(sideToMove, piece, from);
        hash ^= zobrist(sideToMove, piece, to);

        if (capture != 0) {
            if (capture == EN_PASSANT) {
                to = (to & 7) | (from & 56);
                capture = PAWN;
            } else if (capture == ROOK && (flags & CASTLE_MASK) != 0) {
                flags &= CASTLE_REVOKE[to];
            }
            bitBoards[OTHER(sideToMove)] ^= BIT(to);
            bitBoards[capture] ^= BIT(to);
            hash ^= zobrist(OTHER(sideToMove), capture, to);
            counter &= FIFTY_MASK;
            material[OTHER(sideToMove)] -= MATERIAL[capture];
        }

        flags &= CASTLE_MASK;
        counter += INCREMENT_FIFTY_PLY;

        if (piece == PAWN) {
            counter &= FIFTY_MASK;
            if (((from ^ to) & 8) == 0) {
                flags |= from ^ 24;
            } else if (to < 8 || to >= 56) {
                bitBoards[piece] ^= BIT(to);
                bitBoards[Move.promotion(move)] ^= BIT(to);
                hash ^= zobrist(sideToMove, piece, to);
                hash ^= zobrist(sideToMove, Move.promotion(move), to);
                material[sideToMove] += MATERIAL[Move.promotion(move)] - MATERIAL[PAWN];
            }
        } else if (piece == ROOK && (flags & CASTLE_MASK) != 0) {
            flags &= CASTLE_REVOKE[from];
        } else if (piece == KING) {
            kings[sideToMove] = kings[sideToMove] == from ? to : from;
            flags &= ~(CASTLING_MASK << sideToMove);
            if (((from ^ to) & 3) == 2) {
                bitBoards[sideToMove] ^= BIT(Move.from(CASTLE_ROOK[to])) | BIT(Move.to(CASTLE_ROOK[to]));
                bitBoards[ROOK] ^= BIT(Move.from(CASTLE_ROOK[to])) | BIT(Move.to(CASTLE_ROOK[to]));
                hash ^= zobrist(sideToMove, ROOK, Move.from(CASTLE_ROOK[to]));
                hash ^= zobrist(sideToMove, ROOK, Move.to(CASTLE_ROOK[to]));
            }
        }
    }

    private long zobrist(int sideToMove, int piece, int square) {
        return ZOBRIST[sideToMove << 9 | piece << 6 | square];
    }

    private long zobrist(int sideToMove, int flags) {
        return ZOBRIST[0x400 | flags << 1 | sideToMove];
    }

    public static Board load(String fen) {
        Board board = new Board();
        int pos = 56;
        int index = 0;
        while (isSpaceChar(fen.charAt(index))) index++;
        while (!isSpaceChar(fen.charAt(index))) {
            char c = fen.charAt(index++);
            if (c == '/') {
                pos -= 16;
            } else if (Character.isDigit(c)) {
                pos += (c - '0');
            } else {
                int side = Character.isUpperCase(c) ? WHITE : BLACK;
                int piece = Utils.SYMBOLS.indexOf(toUpperCase(c));
                board.bitBoards[side] |= BIT(pos);
                board.bitBoards[piece] |= BIT(pos);
                board.material[side] += MATERIAL[piece];
                board.hash ^= board.zobrist(side, piece, pos);
                if (piece == KING)
                    board.kings[side] = pos;
                pos += 1;
            }
        }
        while (isSpaceChar(fen.charAt(index))) index++;
        board.sideToMove = fen.charAt(index++) == 'w' ? WHITE : BLACK;
        while (isSpaceChar(fen.charAt(index))) index++;
        while (!isSpaceChar(fen.charAt(index))) {
            char c = fen.charAt(index++);
            if (c == 'K') board.flags |= BIT(6);
            if (c == 'k') board.flags |= BIT(7);
            if (c == 'Q') board.flags |= BIT(8);
            if (c == 'q') board.flags |= BIT(9);
        }
        while (isSpaceChar(fen.charAt(index))) index++;
        if (fen.charAt(index++) != '-') {
            int file = fen.charAt(index - 1) - 'a';
            int rank = fen.charAt(index++) - 'a';
            board.flags |= 8 * rank + file;
        }
        int halfMove = 0;
        int fullMove = 1;
        while (fen.length() > index && isSpaceChar(fen.charAt(index))) index++;
        if (fen.length() > index) {
            halfMove = 0;
            while (fen.length() > index && Character.isDigit(fen.charAt(index))) {
                halfMove *= 10;
                halfMove += fen.charAt(index++) - '0';
            }
        }
        while (fen.length() > index && isSpaceChar(fen.charAt(index))) index++;
        if (fen.length() > index) {
            fullMove = 0;
            while (fen.length() > index && Character.isDigit(fen.charAt(index))) {
                fullMove *= 10;
                fullMove += fen.charAt(index++) - '0';
            }
        }
        board.counter = (fullMove - 1) * 2 + board.sideToMove + (halfMove << 10);
        return board;
    }

    private static long[] createZobrist() {
        Random random = new Random(0);
        long[] zobrist = new long[0x800];
        for (int i = 0; i < zobrist.length; i++) {
            zobrist[i] = random.nextLong();
        }
        return zobrist;
    }

    private static int[] createCastleRook() {
        int[] rooks = new int[64];
        rooks[2] = Move.create(0, 3, ROOK, 0, 0, WHITE);
        rooks[6] = Move.create(7, 5, ROOK, 0, 0, WHITE);
        rooks[58] = Move.create(56, 59, ROOK, 0, 0, BLACK);
        rooks[62] = Move.create(63, 61, ROOK, 0, 0, BLACK);
        return rooks;
    }

    private static int[] createCastleRevoke() {
        int[] castle = new int[64];
        for (int i = 0; i < castle.length; i++) {
            castle[i] = -1;
        }
        castle[7] ^= BIT(6);
        castle[63] ^= BIT(7);
        castle[0] ^= BIT(8);
        castle[56] ^= BIT(9);
        return castle;
    }

    public long hash() {
        return hash;
    }

    public int[] getMoveList(int ply) {
        return moveList[ply];
    }

    public long enPassant(int sideToMove) {
        return BIT(flags & EN_PASSANT_MASK) & (sideToMove == BLACK ? 0xFF0000L : 0xFF0000000000L);
    }

    public int enPassantPosition() {
        return flags & EN_PASSANT_MASK;
    }

    public long enPassantPawn(int sideToMove) {
        return sideToMove == BLACK ? enPassant(sideToMove) << 8 : enPassant(sideToMove) >>> 8;
    }

    public long castle() {
        return flags & CASTLE_MASK;
    }

    public int kingPosition(int sideToMove) {
        return kings[sideToMove];
    }

    public void assertBoard() {
        long piecesOr = bitBoards[WHITE] | bitBoards[BLACK];
        long piecesXor = bitBoards[WHITE] ^ bitBoards[BLACK];
        if (piecesOr != piecesXor) throw new IllegalStateException("Board missmatch");
        long individualOr = 0, individualXor = 0;
        for (int i = PAWN; i <= KING; i++) {
            individualOr |= bitBoards[i];
            individualXor ^= bitBoards[i];
        }
        if (individualOr != individualXor) throw new IllegalStateException("Individuals missmatch");
        if (piecesOr != individualOr) throw new IllegalStateException("Global missmatch");
    }

    public void print() {
        System.out.println(DebugUtils.debug(bitBoards));
    }

    public int sideToMove() {
        return sideToMove;
    }
}
