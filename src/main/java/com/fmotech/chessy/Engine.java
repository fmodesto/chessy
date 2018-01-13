package com.fmotech.chessy;

import java.util.ArrayList;
import java.util.List;

import static com.fmotech.chessy.BitOperations.bitCount;
import static com.fmotech.chessy.BitOperations.sparseBitCount;
import static com.fmotech.chessy.Board.BLACK;
import static com.fmotech.chessy.Board.PAWN;
import static com.fmotech.chessy.Board.QUEEN;
import static com.fmotech.chessy.Board.ROOK;
import static com.fmotech.chessy.Board.WHITE;
import static com.fmotech.chessy.Evaluation.PAWN_FREE;
import static com.fmotech.chessy.MoveGenerator.attackingPieces;
import static com.fmotech.chessy.MoveGenerator.pinnedPieces;
import static com.fmotech.chessy.Utils.OTHER;

public class Engine {

    private static final int nullvar[] = new int[]{13, 43, 149, 519, 1809, 6311, 22027};

    private static final int CHECK_NODES = 0xFFFF;

    private BoardTable scoreTable = new BoardTable(0x200000);
    private BoardTable moveTable = new BoardTable(0x400000);

    private int[][] pv = new int[64][64];
    private int[] pvlength = new int[64];

    private long[] boardStack = new long[0x800];

    private int[] killer = new int[128];
    private int[] history = new int[0x1000];

    final int[] value = new int[64];
    int iter = 0;

    private Board board;
    private Evaluation evaluation = new Evaluation();
    private long nodes = 0;
    private long qnodes = 0;

    long searchtime, starttime;
    boolean abort;
    private See see = new See();

    public Engine(Board board) {
        this.board = board;
    }

    public String calc(int time, int depth) {
        long ch = attackingPieces(board.kingPosition(board.sideToMove()), board.sideToMove(), board);;
        int t1 = 0;
        time *= 1000;

        iter = value[0] = 0;
        abort = false;
        qnodes = nodes = 0L;

        searchtime = time;

        starttime = System.currentTimeMillis();

        for (iter = 1; iter <= depth; iter++) {
            value[iter] = search(ch, board.sideToMove(), iter, 0, -32000, 32000, iter != 1, true, false);
            t1 = (int)(System.currentTimeMillis() - starttime);
            if (abort && pvlength[0] == 0 && (iter--) != 0) break;
            if (pvlength[0] > 0) {
                System.out.printf("%2d %5d %6d %9d  %s\n", iter, value[iter], t1, (int)(nodes + qnodes), displaypv());
            }
            if ((iter >= 32000-value[iter] || abort || t1 > searchtime/2)) break;
        }
        System.out.printf("move %s\n", Formatter.moveToFen(pv[0][0]));

        System.out.printf("\nkibitz W: %d Nodes: %d QNodes: %d Evals: %d s: %d knps: %d\n",
                value[iter], nodes, qnodes, -1, t1, (nodes+qnodes)/(t1+1));
        return Formatter.moveToFen(pv[0][0]);
    }

    private String displaypv() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pvlength[0]; i++) {
            sb.append(Formatter.moveToFen(pv[0][i])).append(" ");
        }
        return sb.toString();
    }

    public static List<Long> hashes = new ArrayList<>();

    private int quiesce(long ch, int c, int ply, int alpha, int beta) {
//        check();
        int i, w, best = -32000;
        int oc = OTHER(c);
        int cmat = board.material(c) - board.material(oc);
        if (ply == 63) return evaluation.evaluate(board, c) + cmat;

        if (ch == 0) do {
            if (cmat - 200 >= beta) return beta;
            if (cmat + 200 <= alpha) break;
            best = evaluation.evaluate(board, c) + cmat;
            if (best > alpha) {
                alpha = best;
                if (best >= beta) return beta;
            }
        } while(false);

        long pin = pinnedPieces(board.kingPosition(c), c, board);
        int[] moveList = MoveGenerator.generate(ch, pin, c, ply, true, false, true, board);
        if (ch != 0 && moveList[0] == 1) return -32000 + ply;

        for (i = 1; i < moveList[0]; i++) {
            int m = qpick(moveList, i);
            if (ch == 0 && Move.promotion(m) == 0 && Board.MATERIAL[Move.piece(m)] > Board.MATERIAL[Move.capture(m)] && see.evaluate(board, m) < 0) continue;

            board.doMove(m);
            qnodes++;

            long nch = attackingPieces(board.kingPosition(oc), oc, board);
            w = -quiesce(nch, oc, ply+1, -beta, -alpha);

            board.undoMove(m);

            if (w > best) {
                best = w;
                if (w > alpha) {
                    alpha = w;
                    if (w >= beta) return beta;
                }
            }
        }
        return best > -32000 ? best : evaluation.evaluate(board, c) + cmat;
    }

    private boolean check() {
        if (hashes.size() == 1030968)
            System.out.println("HERE");
        return hashes.add(board.hash());
    }

    private int search(long ch, int c, int d, int ply, int alpha, int beta, boolean followPv, boolean pvNode, boolean allowNullMoves) {
//        check();
        int oc = OTHER(c);
        int w = board.material(c) - board.material(oc);
        boolean continuePv = false;

        pvlength[ply] = ply;
        if (ply == 63) return evaluation.evaluate(board, c) + w;
        if ((++nodes & CHECK_NODES) == 0) {
            long consumed = System.currentTimeMillis() - starttime;
            if (consumed > searchtime) abort = true;
        }
        if (abort) return 0;

        long moveHash = board.hash(c);
        if (ply != 0 && isDraw(moveHash, 1) != 0) return 0;

        if (d == 0) return quiesce(ch, c, ply, alpha, beta);
        boardStack[board.ply()] = moveHash;

        long scoreHash = board.hash(c, d);
        if (scoreTable.containsKey(scoreHash)) {
            w = (scoreTable.get(scoreHash) & 0xFFFF) - 32768;
            if ((scoreTable.get(scoreHash) & 0x10000) != 0) {
                allowNullMoves = false;
                if (w <= alpha) return alpha;
            } else {
                if (w >= beta) return beta;
            }
        }

        long pin = pinnedPieces(board.kingPosition(c), c, board);
        if (!pvNode && ch == 0 && allowNullMoves && d > 1 && bitCount(board.get(c) & ~board.get(PAWN) & ~pin) > 2) {
            int R = (10 + d + nullVariance(w - beta)) / 4;
            if (R > d) R = d;
            board.doNullMove();
            w = -search(0L, oc, d - R, ply + 1, -beta, -alpha, false,false, false); //Null Move Search
            board.undoNullMove();
            if (!abort && w >= beta) {
                scoreTable.put(scoreHash, w + 32768);
                return beta;
            }
        }

        int hmove = 0;
        if (followPv) {
            hmove = retPVMove(c, ply, ch, pin);
            continuePv = hmove != 0 && (ply + 1) != iter;
            if (hmove == 0) hmove = moveTable.get(moveHash);
        } else {
            hmove = moveTable.get(moveHash);
            if (d >= 4 && hmove == 0) { // Simple version of Internal Iterative Deepening
                search(ch, c, d - 3, ply, alpha, beta, false, pvNode, false);
                hmove = moveTable.get(moveHash);
            }
        }

        boolean first = true;
        int[] moveList = board.getMoveList(ply);
        for (int n = 1; n <= ((ch != 0L) ? 2 : 3); n++) {
            if (n == 1) {
                if (hmove == 0) continue;
                moveList[0] = 2;
            } else if (n == 2) {
                MoveGenerator.generate(ch, pin, c, ply, true, false, true, board);
            } else {
                MoveGenerator.generate(ch, pin, c, ply, false, true, true, board);
            }
            for (int i = 1; i < moveList[0]; i++) {
                int m;
                long nch;
                int ext = 0;
                if (n == 1) {
                    m = hmove;
                } else {
                    if (n == 2) m = qpick(moveList, i);
                    else m = spick(moveList, i, ply);
                    if (m == hmove) continue;
                }
                board.doMove(m);

                nch = attackingPieces(board.kingPosition(oc), oc, board);
                if (nch != 0) ext++; // Check Extension
                else if (d >= 3 && n == 3 && !pvNode) { //LMR
                    if (m == killer[ply]) ; //Don't reduce killers
                    else if (Move.piece(m) == PAWN && (PAWN_FREE[c][Move.to(m)] & board.get(PAWN) & board.get(oc)) == 0); //Don't reduce free pawns
                    else ext--;
                }

                if (first && pvNode) {
                    w = -search(nch, oc, d - 1 + ext, ply + 1, -beta, -alpha, continuePv, true, true);
                } else {
                    w = -search(nch, oc, d - 1 + ext, ply + 1, -alpha - 1, -alpha, false, false, true);
                    if (w > alpha && ext < 0)
                        w = -search(nch, oc, d - 1, ply + 1, -alpha - 1, -alpha, false, false, true);
                    if (w > alpha && w < beta && pvNode)
                        w = -search(nch, oc, d - 1 + ext, ply + 1, -beta, -alpha, false, true, true);
                }
                board.undoMove(m);

                if (!abort && w > alpha) {
                    moveTable.put(moveHash, m);
                    alpha = w;
                    if (w >= beta) {
                        if (Move.capture(m) == 0) {
                            killer[ply] = m;
                            history[m & 0xFFF]++;
                        }
                        scoreTable.put(scoreHash, w + 32768); // beta cutoff
                        return beta;
                    }
                    if (pvNode) {
                        pv[ply][ply] = m;
                        System.arraycopy(pv[ply + 1], ply + 1, pv[ply], ply + 1, pvlength[ply + 1] - (ply + 1));
                        pvlength[ply] = pvlength[ply + 1];
                        if (w == 31999 - ply) return w;
                    }
                }
                first = false;
            }
        }
        if (first) return (ch != 0L) ? -32000 + ply : 0;
        if (!abort) scoreTable.put(scoreHash, 0x10000 | (alpha + 32768)); // Alpha cutoff
        return alpha;
    }

    private int nullVariance(int delta) {
        int r = 0;
        if (delta >= 4)
            for (r = 1; r <= nullvar.length; r++)
                if (delta < nullvar[r - 1])
                    break;
        return r;
    }

    private int retPVMove(int c, int ply, long ch, long pin) {
        int[] movelist = MoveGenerator.generate(ch, pin, c, ply, true, true, true, board);
        for (int i = 1; i < movelist[0]; i++) {
            int m = movelist[i];
            if (m == pv[0][ply]) return m;
        }
        return 0;
    }

    /* In normal search some basic move ordering heuristics are used */
    private int spick(int[] ml, int s, int ply) {
        int pi = 0, vmax = -9999;
        for (int i = s; i < ml[0]; i++) {
            int m = ml[i];
            if (m == killer[ply]) {
                pi = i;
                break;
            }
            if (vmax < history[m & 0xFFF]) {
                vmax = history[m & 0xFFF];
                pi = i;
            }
        }
        int m = ml[pi];
        if (pi != s) ml[pi] = ml[s];
        return m;
    }

    /* In quiesce the moves are ordered just for the value of the captured piece */
    private int qpick(int[] ml, int s) {
        int pi = 0, vmax = -9999;
        for (int i = s; i < ml[0]; i++) {
            int m = ml[i];
            int t = Board.MATERIAL[Move.capture(m)];
            if (t > vmax) {
                vmax = t;
                pi = i;
            }
        }
        int m = ml[pi];
        if (pi != s) ml[pi] = ml[s];
        return m;
    }


    private int isDraw(long hp, int nrep) {
        if (board.fifty() > 3) {
            int c = 0, n = board.ply() - board.fifty();
            if (board.fifty() >= 100) return 2; //100 plies
            for (int i = board.ply() - 2; i >= n; i--)
                if (boardStack[i] == hp && ++c == nrep) return 1;
        } else if ((board.get(PAWN) | board.get(ROOK) | board.get(QUEEN)) == 0) { //Check for mating material
            if (sparseBitCount(board.get(WHITE)) <= 2 && sparseBitCount(board.get(BLACK)) <= 2) return 3;
        }
        return 0;
    }
}
