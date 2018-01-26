package com.fmotech.chessy;

import java.util.Random;

import static com.fmotech.chessy.BitOperations.highInt;
import static com.fmotech.chessy.BitOperations.joinInts;
import static com.fmotech.chessy.BitOperations.lowInt;
import static com.fmotech.chessy.BoardUtils.BIT;
import static com.fmotech.chessy.BoardUtils.OTHER;
import static java.lang.Character.isSpaceChar;
import static java.lang.Character.toUpperCase;

public class Board {
    private static final int CASTLE_MASK = 0x3c0;
    private static final int EN_PASSANT_MASK = 0x03f;
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

    public static final int HASH = 8;

    public static final int EN_PASSANT = 1;

    public static final int[] MATERIAL = { 0, 100, 100, 290, 310, 500, 950, 0 };
    private static final int[] CASTLE_REVOKE = createCastleRevoke();
    private static final int[] CASTLE_ROOK = createCastleRook();
    private static final long[] ZOBRIST = createZobrist();
    private static final long[] CASTLE = createCastle();

    public static final Board INIT = Board.load("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

    private long[] bitBoards = new long[10];
    private long[] moveStack = new long[0x400];
    private int[] material = new int[2];
    private int[] kings = new int[2];
    private int[][] moveList = new int[64][256];

    private int flags = 0;
    private int counter = 0;
    private int sideToMove = WHITE;

    private Board() {}

    public int pieceType(int position) {
        long bit = BIT(position);
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
        moveStack[ply()] = joinInts(flags, counter);
        move(move);
        if (Move.promotion(move) != 0) material[Move.sideToMove(move)] += MATERIAL[Move.promotion(move)] - MATERIAL[PAWN];
        if (Move.capture(move) != 0) material[OTHER(Move.sideToMove(move))] -= MATERIAL[Move.capture(move)];
        sideToMove = OTHER(Move.sideToMove(move));
    }

    public void doNullMove() {
        moveStack[ply()] = joinInts(flags, counter);
        flags &= CASTLE_MASK;
        counter += INCREMENT_FIFTY_PLY;
        sideToMove = OTHER(sideToMove);
    }

    public void undoMove(int move) {
        long data = moveStack[ply() - 1];
        move(move);
        if (Move.promotion(move) != 0) material[Move.sideToMove(move)] -= MATERIAL[Move.promotion(move)] - MATERIAL[PAWN];
        if (Move.capture(move) != 0) material[OTHER(Move.sideToMove(move))] += MATERIAL[Move.capture(move)];
        flags = highInt(data);
        counter = lowInt(data);
        sideToMove = Move.sideToMove(move);
    }

    public void undoNullMove() {
        long data = moveStack[ply() - 1];
        flags = highInt(data);
        counter = lowInt(data);
        sideToMove = OTHER(sideToMove);
    }

    private void move(int move) {
        int from = Move.from(move);
        int to = Move.to(move);
        int piece = Move.piece(move);
        int capture = Move.capture(move);
        int sideToMove = Move.sideToMove(move);

        bitBoards[sideToMove] ^= BIT(from) | BIT(to);
        bitBoards[piece] ^= BIT(from) | BIT(to);
        bitBoards[HASH] ^= zobrist(sideToMove, piece, from);
        bitBoards[HASH] ^= zobrist(sideToMove, piece, to);

        if (capture != 0) {
            if (capture == EN_PASSANT) {
                to = (from & 0x38) | (to & 0x07);
                capture = PAWN;
            } else if (capture == ROOK) {
                flags &= CASTLE_REVOKE[to];
            }
            bitBoards[OTHER(sideToMove)] ^= BIT(to);
            bitBoards[capture] ^= BIT(to);
            bitBoards[HASH] ^= zobrist(OTHER(sideToMove), capture, to);
            counter &= FIFTY_MASK;
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
                bitBoards[HASH] ^= zobrist(sideToMove, piece, to);
                bitBoards[HASH] ^= zobrist(sideToMove, Move.promotion(move), to);
            }
        } else if (piece == ROOK) {
            flags &= CASTLE_REVOKE[from];
        } else if (piece == KING) {
            kings[sideToMove] = kings[sideToMove] == from ? to : from;
            flags &= ~(CASTLING_MASK << sideToMove);
            if (((from ^ to) & 3) == 2) {
                bitBoards[sideToMove] ^= BIT(Move.from(CASTLE_ROOK[to])) | BIT(Move.to(CASTLE_ROOK[to]));
                bitBoards[ROOK] ^= BIT(Move.from(CASTLE_ROOK[to])) | BIT(Move.to(CASTLE_ROOK[to]));
                bitBoards[HASH] ^= zobrist(sideToMove, ROOK, Move.from(CASTLE_ROOK[to]));
                bitBoards[HASH] ^= zobrist(sideToMove, ROOK, Move.to(CASTLE_ROOK[to]));
            }
        }
    }

    private long zobrist(int sideToMove, int piece, int square) {
        return ZOBRIST[sideToMove << 9 | piece << 6 | square];
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
                int piece = BoardUtils.SYMBOLS.indexOf(toUpperCase(c));
                board.bitBoards[side] |= BIT(pos);
                board.bitBoards[piece] |= BIT(pos);
                board.material[side] += MATERIAL[piece];
                board.bitBoards[HASH] ^= board.zobrist(side, piece, pos);
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
            int rank = fen.charAt(index++) - '1';
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
        long[] zobrist = new long[0x1000];
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

    private static long[] createCastle() {
        long[] castle = new long[16];
        for (int i = 0; i < castle.length; i++) {
            if ((i & 0x01) != 0) castle[i] |= BIT(7);
            if ((i & 0x02) != 0) castle[i] |= BIT(63);
            if ((i & 0x04) != 0) castle[i] |= BIT(0);
            if ((i & 0x08) != 0) castle[i] |= BIT(56);
        }
        return castle;
    }

    public long hash() {
        return bitBoards[HASH];
    }

    public long hash(int sideToMove) {
        return bitBoards[HASH] ^ ZOBRIST[0x400 | (sideToMove << 11) | flags];
    }

    public long hash(int sideToMove, int depth) {
        return bitBoards[HASH] ^ ZOBRIST[0x400 | flags] ^ ZOBRIST[0x800 | depth << 1 | sideToMove];
    }

    public int[] getMoveList(int ply) {
        return moveList[ply];
    }

    public long enPassant(int sideToMove) {
        return BIT(flags & EN_PASSANT_MASK) & (sideToMove == BLACK ? 0xFF0000L : 0xFF0000000000L);
    }

    public long enPassantPawn(int sideToMove) {
        return sideToMove == BLACK ? enPassant(sideToMove) << 8 : enPassant(sideToMove) >>> 8;
    }

    public long castle(int side) {
        return CASTLE[(flags & CASTLE_MASK) >>> 6] & bitBoards[side];
    }

    public int kingPosition(int sideToMove) {
        return kings[sideToMove];
    }

    public int sideToMove() {
        return sideToMove;
    }

    public int material(int sideToMove) {
        return material[sideToMove];
    }

    public void print() {
        System.out.println(DebugUtils.debug(bitBoards));
    }
}
