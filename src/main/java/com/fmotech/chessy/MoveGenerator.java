package com.fmotech.chessy;

import java.util.HashMap;
import java.util.Map;

import static com.fmotech.chessy.BitOperations.lowestBit;
import static com.fmotech.chessy.BitOperations.lowestBitPosition;
import static com.fmotech.chessy.BitOperations.nextLowestBit;
import static com.fmotech.chessy.Board.BISHOP;
import static com.fmotech.chessy.Board.BLACK;
import static com.fmotech.chessy.Board.EN_PASSANT;
import static com.fmotech.chessy.Board.KING;
import static com.fmotech.chessy.Board.KNIGHT;
import static com.fmotech.chessy.Board.PAWN;
import static com.fmotech.chessy.Board.QUEEN;
import static com.fmotech.chessy.Board.ROOK;
import static com.fmotech.chessy.Board.WHITE;
import static com.fmotech.chessy.MagicBitboard.MASK;
import static com.fmotech.chessy.MagicBitboard.SEGMENT;
import static com.fmotech.chessy.MagicBitboard.bishopRay;
import static com.fmotech.chessy.MagicBitboard.bishopXRay;
import static com.fmotech.chessy.MagicBitboard.rookRay;
import static com.fmotech.chessy.MagicBitboard.rookXRay;
import static com.fmotech.chessy.BoardUtils.BIT;
import static com.fmotech.chessy.BoardUtils.OTHER;
import static com.fmotech.chessy.BoardUtils.RANK;
import static com.fmotech.chessy.BoardUtils.TEST;

public class MoveGenerator {

    private static final int QUEEN_PROMOTIONS = 1;
    private static final int REST_PROMOTIONS = 2;
    private static final int ALL_PROMOTIONS = 3;

    public static long countMoves(int depth, Board board, boolean div) {
        return perft(board.sideToMove(), depth, div, board);
    }

    public static Map<String, Long> moves = new HashMap<>();
    static long perft(int sideToMove, int d, boolean div, Board board) {
        if (d == 0) return 1;
        int ply = 63 - d;
        long count = 0L;
        long check = attackingPieces(board.kingPosition(sideToMove), sideToMove, board);
        long pin = pinnedPieces(board.kingPosition(sideToMove), sideToMove, board);
        int moveList[] = generate(check, pin, sideToMove, ply, true, true, true, board);
        if (d == 1) return moveList[0] - 1;
        if (div) moves.clear();
        for (int i = 1; i < moveList[0]; i++) {
            int move = moveList[i];
            board.doMove(move);
            long n = perft(OTHER(sideToMove), d - 1, false, board);
            count += n;
            if (div) {
                System.out.println(Formatter.moveToFen(move) + " " + n + " " + move);
                moves.put(Formatter.moveToFen(move), n);
            }
            board.undoMove(move);
        }
        return count;
    }

    public static int[] generate(Board board) {
        int sideToMove = board.sideToMove();
        long check = attackingPieces(board.kingPosition(sideToMove), sideToMove, board);
        long pin = pinnedPieces(board.kingPosition(sideToMove), sideToMove, board);
        return generate(check, pin, sideToMove, 0, true, true, true, board);
    }

    public static int[] generate(long check, long pin, int sideToMove, int ply, boolean active, boolean quiet, boolean allPromotions, IBoard board) {
        int[] moves = board.getMoveList(ply);
        moves[0] = 1;

        if (check != 0) {
            generateCheckEscape(moves, sideToMove, check, pin, allPromotions ? ALL_PROMOTIONS : QUEEN_PROMOTIONS, board);
            return moves;
        }
        if (active)
            generateActiveMoves(moves, sideToMove, pin, board);
        if (quiet)
            generateQuietMoves(moves, sideToMove, pin, board);
        return moves;
    }

    private static void generateCheckEscape(int[] moves, int sideToMove, long check, long pin, int flags, IBoard board) {
        long own = board.get(sideToMove);
        long enemy = board.get(OTHER(sideToMove));
        long pieces = own | enemy;
        generateKingMoves(moves, sideToMove, ~own, board);

        if (nextLowestBit(check) != 0) return; // Can't capture several pieces

        int target = lowestBitPosition(check);
        int type = board.pieceType(check);

        long capturers = attackingPieces(target, OTHER(sideToMove), board) & ~pin;
        while (capturers != 0) {
            int from = lowestBitPosition(capturers);
            int piece = board.pieceType(BIT(from));
            if (piece == PAWN) {
                registerPawnMove(moves, sideToMove, from, target, type, flags);
            } else {
                moves[moves[0]++] = Move.create(from, target, piece, type, 0, sideToMove);
            }
            capturers = nextLowestBit(capturers);
        }

        if (board.enPassantPawn(sideToMove) == check) {
            capturers = MagicBitboard.PAWN_ATTACK[OTHER(sideToMove)][board.enPassantPosition()] & board.get(PAWN) & own & ~pin;
            while (capturers != 0) {
                int from = lowestBitPosition(capturers);
                moves[moves[0]++] = Move.create(from, board.enPassantPosition(), PAWN, EN_PASSANT, 0, sideToMove);
                capturers = nextLowestBit(capturers);
            }
        }

        int king = board.kingPosition(sideToMove);
        if ((check & (MagicBitboard.KNIGHT[king] | MagicBitboard.KING[king])) != 0) return; // Can't block

        long segment = SEGMENT(king, target, pieces);
        while (segment != 0) {
            int to = lowestBitPosition(segment);
            long blockers = reach(to, sideToMove, board) & ~pin;
            while (blockers != 0) {
                int from = lowestBitPosition(blockers);
                int piece = board.pieceType(lowestBit(blockers));
                moves[moves[0]++] = Move.create(from, to, piece, 0, 0, sideToMove);
                blockers = nextLowestBit(blockers);
            }
            int from = to + (sideToMove == BLACK ? 8 : -8);
            if (from >= 0 && from < 64) {
                if ((BIT(from) & board.get(PAWN) & own & ~pin) != 0)
                    registerPawnMove(moves, sideToMove, from, to, 0, flags);

                int jump = sideToMove == BLACK ? from + 8 : from - 8;
                if (RANK(to) == (sideToMove == BLACK ? 4 : 3) && (pieces & BIT(from)) == 0
                        && (BIT(jump) & board.get(PAWN) & own & ~pin) != 0)
                    moves[moves[0]++] = Move.create(jump, to, PAWN, 0, 0, sideToMove);
            }
            segment = nextLowestBit(segment);
        }
    }

    private static void generateKingMoves(int[] moves, int sideToMove, long mask, IBoard board) {
        long enemy = board.get(OTHER(sideToMove));
        int king = board.kingPosition(sideToMove);

        long next = MagicBitboard.KING[king] & mask;
        while (next != 0) {
            int to = lowestBitPosition(next);
            long bit = BIT(to);
            if (!positionAttacked(to, sideToMove, board))
                moves[moves[0]++] = Move.create(king, to, KING, (bit & enemy) != 0 ? board.pieceType(bit) : 0, 0, sideToMove);
            next = nextLowestBit(next);
        }
    }

    public static void generateQuietMoves(int[] moves, int sideToMove, long pin, IBoard board) {
        long own = board.get(sideToMove);
        long enemy = board.get(OTHER(sideToMove));
        long pieces = own | enemy;
        int king = board.kingPosition(sideToMove);

        long next = board.get(PAWN) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long mask = TEST(from, pin) ? MASK(from, king) : -1;
            int delta = sideToMove == WHITE ? 8 : -8;
            int to = from + delta;
            if (!TEST(to, pieces | ~mask)) {
                registerPawnMove(moves, sideToMove, from, to, 0, REST_PROMOTIONS);
                if (RANK(from) == (sideToMove == WHITE ? 1 : 6)) {
                    to += delta;
                    if (!TEST(to, pieces | ~mask))
                        moves[moves[0]++] = Move.create(from, to, PAWN, 0, 0, sideToMove);
                }
            }
            if (RANK(from) == (sideToMove == BLACK ? 1 : 6)) {
                long target = MagicBitboard.PAWN_ATTACK[sideToMove][from] & enemy & mask;
                registerAttackMoves(moves, sideToMove, from, PAWN, target, ROOK, board);
                registerAttackMoves(moves, sideToMove, from, PAWN, target, BISHOP, board);
                registerAttackMoves(moves, sideToMove, from, PAWN, target, KNIGHT, board);
            }
            next = nextLowestBit(next);
        }

        next = board.get(KNIGHT) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long mask = TEST(from, pin) ? MASK(from, king) : -1;
            long target = MagicBitboard.KNIGHT[from] & ~pieces & mask;
            registerMoves(moves, sideToMove, from, KNIGHT, target);
            next = nextLowestBit(next);
        }

        next = board.get(BISHOP) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long mask = TEST(from, pin) ? MASK(from, king) : -1;
            long target = bishopRay(from, pieces) & ~pieces & mask;
            registerMoves(moves, sideToMove, from, BISHOP, target);
            next = nextLowestBit(next);
        }

        next = board.get(ROOK) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long mask = TEST(from, pin) ? MASK(from, king) : -1;
            long target = rookRay(from, pieces) & ~pieces & mask;
            registerMoves(moves, sideToMove, from, ROOK, target);
            if (board.castle() != 0) {
                if (sideToMove == BLACK) {
                    if ((board.castle() & 128) != 0 && (from == 63) && (rookRay(63, pieces) & BIT(60)) != 0
                            && !(positionAttacked(61, sideToMove, board) | positionAttacked(62, sideToMove, board))) {
                        moves[moves[0]++] = Move.create(60, 62, KING, 0, 0, sideToMove);
                    }
                    if ((board.castle() & 512) != 0 && (from == 56) && (rookRay(56, pieces) & BIT(60)) != 0
                            && !(positionAttacked(59, sideToMove, board) | positionAttacked(58, sideToMove, board))) {
                        moves[moves[0]++] = Move.create(60, 58, KING, 0, 0, sideToMove);
                    }
                } else {
                    if ((board.castle() & 64) != 0 && (from == 7) && (rookRay(7, pieces) & BIT(4)) != 0
                            && !(positionAttacked(5, sideToMove, board) | positionAttacked(6, sideToMove, board))) {
                        moves[moves[0]++] = Move.create(4, 6, KING, 0, 0, sideToMove);
                    }
                    if ((board.castle() & 256) != 0 && (from == 0) && (rookRay(0, pieces) & BIT(4)) != 0
                            && !(positionAttacked(3, sideToMove, board) | positionAttacked(2, sideToMove, board))) {
                        moves[moves[0]++] = Move.create(4, 2, KING, 0, 0, sideToMove);
                    }
                }
            }
            next = nextLowestBit(next);
        }

        next = board.get(QUEEN) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long mask = TEST(from, pin) ? MASK(from, king) : -1;
            long target = (rookRay(from, pieces) | bishopRay(from, pieces)) & ~pieces & mask;
            registerMoves(moves, sideToMove, from, QUEEN, target);
            next = nextLowestBit(next);
        }

        generateKingMoves(moves, sideToMove, ~pieces, board);
    }

    public static void generateActiveMoves(int[] moves, int sideToMove, long pin, IBoard board) {
        long own = board.get(sideToMove);
        long enemy = board.get(OTHER(sideToMove));
        long pieces = own | enemy;
        long enPassant = board.enPassant(sideToMove);
        int king = board.kingPosition(sideToMove);

        long next = board.get(PAWN) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long mask = TEST(from, pin) ? MASK(from, king) : -1;
            long target = MagicBitboard.PAWN_ATTACK[sideToMove][from] & (enPassant | enemy) & mask;
            if ((target & enPassant) != 0) {
                long ray = rookRay(from, pieces ^ board.enPassantPawn(sideToMove)) & (sideToMove == WHITE ? 0x000000ff00000000L : 0x00000000ff000000L);
                if (TEST(king, ray) && (ray & (board.get(ROOK) | board.get(QUEEN)) & enemy) != 0) {
                    target ^= enPassant;
                }
            }
            if (RANK(from) == (sideToMove == BLACK ? 1 : 6)) {
                registerAttackMoves(moves, sideToMove, from, PAWN, target, QUEEN, board);
                int to = from + (sideToMove == WHITE ? 8 : -8);
                if (!TEST(to, pieces | ~mask)) {
                    moves[moves[0]++] = Move.create(from, to, PAWN, 0, QUEEN, sideToMove);
                }
            } else {
                registerAttackMoves(moves, sideToMove, from, PAWN, target, 0, board);
            }
            next = nextLowestBit(next);
        }

        next = board.get(KNIGHT) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long mask = TEST(from, pin) ? MASK(from, king) : -1;
            long target = MagicBitboard.KNIGHT[from] & enemy & mask;
            registerAttackMoves(moves, sideToMove, from, KNIGHT, target, 0, board);
            next = nextLowestBit(next);
        }

        next = board.get(BISHOP) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long mask = TEST(from, pin) ? MASK(from, king) : -1;
            long target = bishopRay(from, pieces) & enemy & mask;
            registerAttackMoves(moves, sideToMove, from, BISHOP, target, 0, board);
            next = nextLowestBit(next);
        }

        next = board.get(ROOK) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long mask = TEST(from, pin) ? MASK(from, king) : -1;
            long target = rookRay(from, pieces) & enemy & mask;
            registerAttackMoves(moves, sideToMove, from, ROOK, target, 0, board);
            next = nextLowestBit(next);
        }

        next = board.get(QUEEN) & own;
        while (next != 0) {
            int from = lowestBitPosition(next);
            long mask = TEST(from, pin) ? MASK(from, king) : -1;
            long target = (rookRay(from, pieces) | bishopRay(from, pieces)) & enemy & mask;
            registerAttackMoves(moves, sideToMove, from, QUEEN, target, 0, board);
            next = nextLowestBit(next);
        }

        generateKingMoves(moves, sideToMove, enemy, board);
    }

    private static void registerMoves(int[] moves, int sideToMove, int from, int piece, long target) {
        while (target != 0) {
            int to = lowestBitPosition(target);
            moves[moves[0]++] = Move.create(from, to, piece, 0, 0, sideToMove);
            target = nextLowestBit(target);
        }
    }

    private static void registerPawnMove(int[] moves, int sideToMove, int from, int to, int capture, int flags) {
        boolean promotion = RANK(from) == (sideToMove == BLACK ? 1 : 6);
        if (promotion & (flags & QUEEN_PROMOTIONS) != 0) {
            moves[moves[0]++] = Move.create(from, to, PAWN, capture, QUEEN, sideToMove);
        }
        if (promotion & (flags & REST_PROMOTIONS) != 0) {
            moves[moves[0]++] = Move.create(from, to, PAWN, capture, ROOK, sideToMove);
            moves[moves[0]++] = Move.create(from, to, PAWN, capture, BISHOP, sideToMove);
            moves[moves[0]++] = Move.create(from, to, PAWN, capture, KNIGHT, sideToMove);
        }
        if (!promotion) {
            moves[moves[0]++] = Move.create(from, to, PAWN, capture, 0, sideToMove);
        }
    }

    private static void registerAttackMoves(int[] moves, int sideToMove, int from, int piece, long target, int promotion, IBoard board) {
        while (target != 0) {
            int to = lowestBitPosition(target);
            long capture = BIT(to);
            moves[moves[0]++] = Move.create(from, to, piece, board.pieceType(capture), promotion, sideToMove);
            target = nextLowestBit(target);
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    public static boolean positionAttacked(int position, int sideToMove, IBoard board) {
        long pieces = (board.get(WHITE) | board.get(BLACK)) ^ BIT(board.kingPosition(sideToMove));
        long enemy = board.get(OTHER(sideToMove));
        if ((MagicBitboard.PAWN_ATTACK[sideToMove][position] & enemy & board.get(PAWN)) != 0) return true;
        if ((MagicBitboard.KNIGHT[position] & enemy & board.get(KNIGHT)) != 0) return true;
        if ((MagicBitboard.KING[position] & enemy & board.get(KING)) != 0) return true;
        if ((rookRay(position, pieces) & enemy & (board.get(ROOK) | board.get(QUEEN))) != 0) return true;
        if ((bishopRay(position, pieces) & enemy & (board.get(BISHOP) | board.get(QUEEN))) != 0) return true;
        return false;
    }

    public static long attackingPieces(int position, int sideToMove, IBoard board) {
        long enemy = board.get(OTHER(sideToMove));
        long pieces = board.get(sideToMove) | enemy;
        return MagicBitboard.PAWN_ATTACK[sideToMove][position] & board.get(PAWN) & enemy
                | MagicBitboard.KNIGHT[position] & board.get(KNIGHT) & enemy
                | rookRay(position, pieces) & (board.get(ROOK) | board.get(QUEEN)) & enemy
                | bishopRay(position, pieces) & (board.get(BISHOP) | board.get(QUEEN)) & enemy;
    }

    private static long reach(int position, int sideToMove, IBoard board) {
        long own = board.get(sideToMove);
        long pieces = board.get(OTHER(sideToMove)) | own;
        return MagicBitboard.KNIGHT[position] & board.get(KNIGHT) & own
                | rookRay(position, pieces) & (board.get(ROOK) | board.get(QUEEN)) & own
                | bishopRay(position, pieces) & (board.get(BISHOP) | board.get(QUEEN)) & own;
    }

    public static long pinnedPieces(int position, int sideToMove, IBoard board) {
        long own = board.get(sideToMove);
        long enemy = board.get(OTHER(sideToMove));
        long pieces = own | enemy;
        long pin = 0L;
        long rookQueen = board.get(ROOK) | board.get(QUEEN);
        long bishopQueen = board.get(BISHOP) | board.get(QUEEN);

        long next = rookXRay(position, pieces) & rookQueen & enemy;
        while (next != 0) {
            int pinner = lowestBitPosition(next);
            pin |= rookRay(pinner, pieces) & rookRay(position, pieces) & own;
            next = nextLowestBit(next);
        }
        next = bishopXRay(position, pieces) & bishopQueen & enemy;
        while (next != 0) {
            int pinner = lowestBitPosition(next);
            pin |= bishopRay(pinner, pieces) & bishopRay(position, pieces) & own;
            next = nextLowestBit(next);
        }
        return pin;
    }
}
