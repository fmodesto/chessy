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

    private static final int HSIZEB = 0x200000;
    private static final int HMASKB = HSIZEB - 1;
    private static final long HINVB = 0xFFFFFFFF00000000L | (0xFFFFFFFFL & ~HMASKB);

    private static final int HSIZEP = 0x400000;
    private static final int HMASKP = HSIZEP - 1;
    private static final long HINVP = 0xFFFFFFFF00000000L | (0xFFFFFFFFL & ~HMASKP);

    private static final int CNODES = 0xFFFF;

    private long[] hashDB = new long[HSIZEB];
    private long[] hashDP = new long[HSIZEP];

    private int[][] pv = new int[64][64];
    private int[] pvlength = new int[64];

    private long[] hstack = new long[0x800];
    private long[] mstack = new long[0x800];

    private int[] killer = new int[128];
    private int[] history = new int[0x1000];

    final static int[] value = new int[64];
    int iter = 0;

    private Board board;
    private Evaluation evaluation = new Evaluation();
    private long nodes = 0;
    private long qnodes = 0;

    static long searchtime, maxtime, starttime;
    boolean sabort, noabort;
    static boolean ponder = false, pondering = false;
    private See see = new See();

    public Engine(Board board) {
        this.board = board;
    }

    public static long[][] mem = new long[20][HSIZEB];

    public String calc(int time, int depth) {
        long ch = attackingPieces(board.kingPosition(board.sideToMove()), board.sideToMove(), board);;
        int t1 = 0;
        time *= 1000;

        iter = value[0] = 0;
        sabort = false;
        qnodes = nodes = 0L;

        searchtime = time;
        maxtime = time;

        starttime = System.currentTimeMillis();
        System.out.println(depth);

        for (iter = 1; iter <= depth; iter++) {
            noabort = false;
            value[iter] = search(ch, board.sideToMove(), iter, 0, -32000, 32000, 1, 0);
            t1 = (int)(System.currentTimeMillis() - starttime);
            if (sabort && pvlength[0] == 0 && (iter--) != 0) break;
            if (pvlength[0] > 0) {
                System.out.printf("%2d %5d %6d %9d  %s\n", iter, value[iter], t1, (int)(nodes + qnodes), displaypv());
            }
            System.arraycopy(hashDB, 0, mem[iter], 0, hashDB.length);
            if ((iter >= 32000-value[iter] || sabort || t1 > searchtime/2)) break;
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
        return hashes.add(board.hash());
    }

    private int search(long ch, int c, int d, int ply, int alpha, int beta, int pvnode, int isnull) {
//        check();
        int oc = OTHER(c);
        int w = board.material(c) - board.material(oc);

        pvlength[ply] = ply;
        if (ply == 63) return evaluation.evaluate(board, c) + w;
        if ((++nodes & CNODES) == 0) {
            long consumed = System.currentTimeMillis() - starttime;
            if (!pondering && (consumed > maxtime || (consumed > searchtime && !noabort))) sabort = true;
        }
        if (sabort) return 0;

        long hp = board.hash(c);
        if (ply != 0 && isDraw(hp, 1) != 0) return 0;

        if (d == 0) return quiesce(ch, c, ply, alpha, beta);
        hstack[board.ply()] = hp;

        long hb = board.hash(c, d);
        long he = hashDB[(int) (hb & HMASKB)];
        if (((he ^ hb) & HINVB) == 0) {
            w = ((int) (he & 0xFFFF)) - 32768;
            if ((he & 0x10000) != 0) {
                isnull = 0;
                if (w <= alpha) return alpha;
            } else {
                if (w >= beta) return beta;
            }
        }

        long pin = pinnedPieces(board.kingPosition(c), c, board);
        if (pvnode == 0 && ch == 0 && isnull != 0 && d > 1 && bitCount(board.get(c) & ~board.get(PAWN) & ~pin) > 2) {
            int R = (10 + d + nullVariance(w - beta)) / 4;
            if (R > d) R = d;
            board.doNullMove();
            w = -search(0L, oc, d - R, ply + 1, -beta, -alpha, 0, 0); //Null Move Search
            board.undoNullMove();
            if (!sabort && w >= beta) {
                hashDB[(int) (hb & HMASKB)] = (hb & HINVB) | (w + 32768);
                return beta;
            }
        }

        int hmove = 0;
        if (ply > 0) {
            he = hashDP[(int) (hp & HMASKP)];
            if (((he ^ hp) & HINVP) == 0) hmove = (int) (he & HMASKP);

            if (d >= 4 && hmove == 0) { // Simple version of Internal Iterative Deepening
                w = search(ch, c, d - 3, ply, alpha, beta, pvnode, 0);
                he = hashDP[(int) (hp & HMASKP)];
                if (((he ^ hp) & HINVP) == 0) hmove = (int) (he & HMASKP);
            }
        } else {
            hmove = retPVMove(c, ply, ch, pin);
        }

        int best = pvnode != 0 ? alpha : -32001;
        int asave = alpha;
        int first = 1;
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
                else if (d >= 3 && n == 3 && pvnode == 0) { //LMR
                    if (m == killer[ply]) ; //Don't reduce killers
                    else if (Move.piece(m) == PAWN && (PAWN_FREE[c][Move.to(m)] & board.get(PAWN) & board.get(oc)) == 0); //Don't reduce free pawns
                    else ext--;
                }

                if (first != 0 && pvnode != 0) {
                    w = -search(nch, oc, d - 1 + ext, ply + 1, -beta, -alpha, 1, 1);
                    if (ply == 0) noabort = (iter > 1 && w < value[iter - 1] - 40);
                } else {
                    w = -search(nch, oc, d - 1 + ext, ply + 1, -alpha - 1, -alpha, 0, 1);
                    if (w > alpha && ext < 0)
                        w = -search(nch, oc, d - 1, ply + 1, -alpha - 1, -alpha, 0, 1);
                    if (w > alpha && w < beta && pvnode != 0)
                        w = -search(nch, oc, d - 1 + ext, ply + 1, -beta, -alpha, 1, 1);
                }
                board.undoMove(m);

                if (!sabort && w > best) {
                    if (w > alpha) {
                        hashDP[(int) (hp & HMASKP)] = (hp & HINVP) | m;
                        alpha = w;
                    }
                    if (w >= beta) {
                        if (Move.capture(m) == 0) {
                            killer[ply] = m;
                            history[m & 0xFFF]++;
                        }
                        hashDB[(int) (hb & HMASKB)] = (hb & HINVB) | (w + 32768);
                        return beta;
                    }
                    if (pvnode != 0 && w >= alpha) {
                        pv[ply][ply] = m;
                        System.arraycopy(pv[ply + 1], ply + 1, pv[ply], ply + 1, pvlength[ply + 1] - (ply + 1));
                        pvlength[ply] = pvlength[ply + 1];
                        if (ply == 0 && iter > 1 && w > value[iter - 1] - 20) noabort = false;
                        if (w == 31999 - ply) return w;
                    }
                    best = w;
                }
                first = 0;
            }
        }
        if (first != 0) return (ch != 0L) ? -32000 + ply : 0;
        if (pvnode != 0) {
            if (!sabort && asave == alpha) hashDB[(int) (hb & HMASKB)] = (hb & HINVB) | 0x10000 | (asave + 32768);
        } else {
            if (!sabort && best < beta) hashDB[(int) (hb & HMASKB)] = (hb & HINVB) | 0x10000 | (best + 32768);
        }
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
        int m;
        int i, pi = 0, vmax = -9999;
        for (i = s; i < ml[0]; i++) {
            m = ml[i];
            if (m == killer[ply]) {
                pi = i;
                break;
            }
            if (vmax < history[m & 0xFFF]) {
                vmax = history[m & 0xFFF];
                pi = i;
            }
        }
        m = ml[pi];
        if (pi != s) ml[pi] = ml[s];
        return m;
    }

    /* In quiesce the moves are ordered just for the value of the captured piece */
    private int qpick(int[] ml, int s) {
        int m;
        int i, t, pi = 0, vmax = -9999;
        for (i = s; i < ml[0]; i++) {
            m = ml[i];
            t = Board.MATERIAL[Move.capture(m)];
            if (t > vmax) {
                vmax = t;
                pi = i;
            }
        }
        m = ml[pi];
        if (pi != s) ml[pi] = ml[s];
        return m;
    }


    private int isDraw(long hp, int nrep) {
        if (board.fifty() > 3) {
            int c = 0, n = board.ply() - board.fifty();
            if (board.fifty() >= 100) return 2; //100 plies
            for (int i = board.ply() - 2; i >= n; i--)
                if (hstack[i] == hp && ++c == nrep) return 1;
        } else if ((board.get(PAWN) | board.get(ROOK) | board.get(QUEEN)) == 0) { //Check for mating material
            if (sparseBitCount(board.get(WHITE)) <= 2 && sparseBitCount(board.get(BLACK)) <= 2) return 3;
        }
        return 0;
    }
}
