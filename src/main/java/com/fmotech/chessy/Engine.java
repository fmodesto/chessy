//package com.fmotech.chessy;
//
//import java.util.Arrays;
//
//import static com.fmotech.chessy.BitOperations.bitCount;
//import static com.fmotech.chessy.BitOperations.sparseBitCount;
//import static com.fmotech.chessy.Board.BLACK;
//import static com.fmotech.chessy.Board.MATERIAL;
//import static com.fmotech.chessy.Board.PAWN;
//import static com.fmotech.chessy.Board.QUEEN;
//import static com.fmotech.chessy.Board.ROOK;
//import static com.fmotech.chessy.Board.WHITE;
//import static com.fmotech.chessy.BoardUtils.OTHER;
//import static java.lang.Integer.signum;
//import static java.lang.Math.abs;
//
//public class Engine {
//
//    private static final int NULL_VARIANCE[] = new int[] {4, 13, 43, 149, 519, 1809, 6311, 22027};
//
//    private static final int CHECK_NODES = 0xFFFF;
//
//    private static final int HASH_MOVE = 1;
//    private static final int ACTIVE_MOVE = 2;
//    private static final int QUIET_MOVE = 3;
//    public static final int INFINITE = 32000;
//
//    private HashTable scoreTable = new HashTable(0x800000);
//    private HashTable moveTable = new HashTable(0x1000000);
//
//    private int[] pvLength = new int[64];
//    private int[][] pv = new int[64][64];
//
//    private long[] boardStack = new long[0x800];
//
//    private int[] killer = new int[128];
//    private int[] history = new int[0x1000];
//
//    private Board board;
//    private Evaluation evaluation = new Evaluation();
//    private See see = new See();
//
//    private long nodes = 0;
//    private long qnodes = 0;
//
//    private long endTime;
//    private boolean abort;
//
//    public Engine(Board board) {
//        this.board = board;
//    }
//
//    public void setup(String fen, String... moves) {
//        Arrays.fill(killer, 0);
//        Arrays.fill(boardStack, 0);
//        Arrays.fill(history, 0);
//        board = Board.load(fen);
//        for (int i = 0; i < moves.length; i++) {
//            String move = moves[i];
//            boardStack[board.ply()] = board.hash(board.sideToMove());
//            board.doMove(Formatter.moveFromFen(board, move));
//        }
//        board.print();
//    }
//
//    public int sideToMove() {
//        return board.sideToMove();
//    }
//
//    public String think(int time, int depth) {
//        abort = false;
//        nodes = 0;
//        qnodes = 0;
//
//        long startTime = System.currentTimeMillis();
//        endTime = startTime + time;
//
//        long ch = attackingPieces(board.kingPosition(board.sideToMove()), board.sideToMove(), board);
//        for (int i = 1; i <= depth; i++) {
//            int score = search(ch, board.sideToMove(), i, 0, -INFINITE, INFINITE, i != 1, true, false);
//            long totalTime = System.currentTimeMillis() - startTime;
//            if (pvLength[0] > 0)
//                outputPvLine(score, i, totalTime);
//            if (abort || i >= INFINITE - score || totalTime > time / 2)
//                break;
//        }
//        return Formatter.moveToFen(pv[0][0]);
//    }
//
//    private void outputPvLine(int score, int depth, long time) {
//        String sc = "cp " + score;
//        if (Math.abs(score) > INFINITE - 100) {
//            int sign = signum(score);
//            sc = "mate " + sign * (INFINITE - abs(score));
//        }
//        System.out.printf("info score %s depth %d nodes %d time %d pv %s\n", sc, depth, nodes + qnodes, time, displaypv());
//    }
//
//    private String displaypv() {
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < pvLength[0]; i++) {
//            sb.append(Formatter.moveToFen(pv[0][i])).append(" ");
//        }
//        return sb.toString();
//    }
//
//    private int search(long check, int side, int depth, int ply, int alpha, int beta, boolean followPv, boolean pvNode, boolean allowNullMoves) {
//        int otherSide = OTHER(side);
//        int score = board.material(side) - board.material(otherSide);
//        boolean continuePv = false;
//
//        pvLength[ply] = ply;
//        if (ply >= 63) return evaluation.evaluate(board, side) + score;
//        if ((++nodes & CHECK_NODES) == 0) {
//            if (System.currentTimeMillis() > endTime)
//                abort = true;
//        }
//        if (abort) return 0;
//
//        long moveHash = board.hash(side);
//        if (ply != 0 && isDraw(moveHash, 1) != 0) return 0;
//
//        if (depth <= 0) return quiescence(check, side, ply, alpha, beta);
//        boardStack[board.ply()] = moveHash;
//
//        long scoreHash = board.hash(side, depth);
//        if (scoreTable.containsKey(scoreHash)) {
//            score = (scoreTable.get(scoreHash) & 0xFFFF) - 32768;
//            if ((scoreTable.get(scoreHash) & 0x10000) != 0) {
//                allowNullMoves = false;
//                if (score <= alpha) return alpha;
//            } else {
//                if (score >= beta) return beta;
//            }
//        }
//
//        long pin = pinnedPieces(board.kingPosition(side), side, board);
//        if (!pvNode && check == 0 && allowNullMoves && depth > 1 && bitCount(board.get(side) & ~board.get(PAWN) & ~pin) > 2) {
//            int R = (10 + depth + nullVariance(score - beta)) / 4;
//            if (R > depth) R = depth;
//            board.doNullMove();
//            score = -search(0L, otherSide, depth - R, ply + 1, -beta, -alpha, false,false, false); //Null Move Search
//            board.undoNullMove();
//            if (!abort && score >= beta) {
//                scoreTable.put(scoreHash, score + 32768);
//                return beta;
//            }
//        }
//
//        int hashMove = 0;
//        if (followPv) {
//            hashMove = retPVMove(side, ply, check, pin);
//            continuePv = hashMove != 0;
//        }
//        if (hashMove == 0) {
//            hashMove = moveTable.get(moveHash);
//            if (depth >= 4 && hashMove == 0) { // Simple version of Internal Iterative Deepening
//                search(check, side, depth - 3, ply, alpha, beta, false, pvNode, false);
//                hashMove = moveTable.get(moveHash);
//            }
//        }
//
//        boolean first = true;
//        int[] moveList = board.getMoveList(ply);
//        for (int phase = 1; phase <= (check != 0 ? 2 : 3); phase++) {
//            if (phase == 1) {
//                if (hashMove == 0) continue;
//                moveList[0] = 2;
//            } else if (phase == 2) {
//                MoveGenerator.generate(check, pin, side, ply, true, false, true, board);
//            } else {
//                MoveGenerator.generate(check, pin, side, ply, false, true, true, board);
//            }
//
//            for (int i = 1; i < moveList[0]; i++) {
//                int move;
//                if (phase == HASH_MOVE) {
//                    move = hashMove;
//                } else if (phase == ACTIVE_MOVE) {
//                    move = nextActiveMove(moveList, i);
//                    if (move == hashMove) continue;
//                } else {
//                    move = nextQuietMove(moveList, i, ply);
//                    if (move == hashMove) continue;
//                }
//
//                board.doMove(move);
//
//                int ext = 0;
//                long enemyCheck = attackingPieces(board.kingPosition(otherSide), otherSide, board);
//                if (enemyCheck != 0)
//                    ext += 1; // Check Extension
//                else if (phase == QUIET_MOVE && depth >= 3 && !pvNode && move != killer[ply] && !isFreePawn(move, side))
//                    ext -= 1; // LMR
//
//                if (first && pvNode) {
//                    score = -search(enemyCheck, otherSide, depth - 1 + ext, ply + 1, -beta, -alpha, continuePv, true, true);
//                } else {
//                    score = -search(enemyCheck, otherSide, depth - 1 + ext, ply + 1, -alpha - 1, -alpha, false, false, true);
//                    if (score > alpha && ext < 0)
//                        score = -search(enemyCheck, otherSide, depth - 1, ply + 1, -alpha - 1, -alpha, false, false, true);
//                    if (score > alpha && score < beta && pvNode)
//                        score = -search(enemyCheck, otherSide, depth - 1 + ext, ply + 1, -beta, -alpha, false, true, true);
//                }
//
//                board.undoMove(move);
//
//                if (!abort && score > alpha) {
//                    moveTable.put(moveHash, move);
//                    alpha = score;
//                    if (score >= beta) {
//                        if (Move.capture(move) == 0) {
//                            killer[ply] = move;
//                            history[move & 0xFFF]++;
//                        }
//                        scoreTable.put(scoreHash, score + 32768); // beta cutoff
//                        return beta;
//                    }
//                    if (pvNode) {
//                        pv[ply][ply] = move;
//                        System.arraycopy(pv[ply + 1], ply + 1, pv[ply], ply + 1, pvLength[ply + 1] - (ply + 1));
//                        pvLength[ply] = pvLength[ply + 1];
//                        if (score == INFINITE - (ply + 1))
//                            return score;
//                    }
//                }
//                first = false;
//            }
//        }
//        if (first) return (check != 0L) ? -INFINITE + ply : 0;
//        if (!abort) scoreTable.put(scoreHash, 0x10000 | (alpha + 32768)); // Alpha cutoff
//        return alpha;
//    }
//
//    private boolean isFreePawn(int move, int side) {
//        return Move.piece(move) == PAWN && (PAWN_FREE[side][Move.to(move)] & board.get(PAWN) & board.get(OTHER(side))) == 0;
//    }
//
//    private int nullVariance(int delta) {
//        for (int r = 0; r < NULL_VARIANCE.length; r++) {
//            if (delta < NULL_VARIANCE[r])
//                return r;
//        }
//        return NULL_VARIANCE.length;
//    }
//
//    private int retPVMove(int side, int ply, long check, long pin) {
//        int[] movelist = MoveGenerator.generate(check, pin, side, ply, true, true, true, board);
//        for (int i = 1; i < movelist[0]; i++) {
//            int move = movelist[i];
//            if (move == pv[0][ply]) return move;
//        }
//        return 0;
//    }
//
//
//    private int quiescence(long check, int side, int ply, int alpha, int beta) {
//        int otherSide = OTHER(side);
//        int material = board.material(side) - board.material(otherSide);
//        if (ply == 63) return evaluation.evaluate(board, side) + material;
//
//        if (check == 0 && material - 200 >= beta) {
//            return beta;
//        }
//        if (check == 0 && material + 200 > alpha) {
//            int score = evaluation.evaluate(board, side) + material;
//            if (score >= beta)
//                return beta;
//            if (score > alpha)
//                alpha = score;
//        }
//
//        long pin = pinnedPieces(board.kingPosition(side), side, board);
//        int[] moveList = MoveGenerator.generate(check, pin, side, ply, true, false, true, board);
//        if (moveList[0] == 1) return check != 0 ? -INFINITE + ply : evaluation.evaluate(board, side) + material;
//
//        for (int i = 1; i < moveList[0]; i++) {
//            int move = nextActiveMove(moveList, i);
//            if (check == 0 && Move.promotion(move) == 0 && MATERIAL[Move.piece(move)] > MATERIAL[Move.capture(move)] && see.evaluate(board, move) < 0)
//                continue;
//
//            board.doMove(move);
//            qnodes++;
//
//            long enemyCheck = attackingPieces(board.kingPosition(otherSide), otherSide, board);
//            int score = -quiescence(enemyCheck, otherSide, ply+1, -beta, -alpha);
//
//            board.undoMove(move);
//
//            if (score >= beta)
//                return beta;
//            if (score > alpha)
//                alpha = score;
//        }
//        return alpha;
//    }
//
//    /* In normal search some basic move ordering heuristics are used */
//    private int nextQuietMove(int[] moves, int start, int ply) {
//        int index = 0;
//        int max = Integer.MIN_VALUE;
//        for (int i = start; i < moves[0]; i++) {
//            if (moves[i] == killer[ply]) {
//                index = i;
//                break;
//            }
//            int t = history[moves[i] & 0xFFF];
//            if (max < t) {
//                max = t;
//                index = i;
//            }
//        }
//        int move = moves[index];
//        moves[index] = moves[start];
//        return move;
//    }
//
//    /* In quiesce the moves are ordered just for the value of the captured piece */
//    private int nextActiveMove(int[] moves, int start) {
//        int index = 0;
//        int max = Integer.MIN_VALUE;
//        for (int i = start; i < moves[0]; i++) {
//            int t = MATERIAL[Move.capture(moves[i])];
//            if (t > max) {
//                max = t;
//                index = i;
//            }
//        }
//        int move = moves[index];
//        moves[index] = moves[start];
//        return move;
//    }
//
//    private int isDraw(long hp, int nrep) {
//        if (board.fifty() > 3) {
//            int c = 0, n = board.ply() - board.fifty();
//            if (board.fifty() >= 100) return 2; //100 plies
//            for (int i = board.ply() - 2; i >= n; i--)
//                if (boardStack[i] == hp && ++c == nrep) return 1;
//        } else if ((board.get(PAWN) | board.get(ROOK) | board.get(QUEEN)) == 0) { //Check for mating material
//            if (sparseBitCount(board.get(WHITE)) <= 2 && sparseBitCount(board.get(BLACK)) <= 2) return 3;
//        }
//        return 0;
//    }
//}
