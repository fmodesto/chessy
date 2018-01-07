///* OliThink5 Java(c) Oliver Brausch 04.Jan.2018, ob112@web.de, http://brausch.org */
//package com.fmotech.chessy.oli;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.lang.reflect.Method;
//import java.security.AccessControlException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Random;
//import java.util.StringTokenizer;
//
//public class OliThink2 {
//	final static int HSIZEB = 0x200000;
//	final static int HMASKB = HSIZEB - 1;
//	final static long HINVB = 0xFFFFFFFF00000000L | (0xFFFFFFFFL & ~HMASKB);
//
//	final static int HSIZEP = 0x400000;
//	final static int HMASKP = HSIZEP - 1;
//	final static long HINVP = 0xFFFFFFFF00000000L | (0xFFFFFFFFL & ~HMASKP);
//	final static long[] hashDB = new long[HSIZEB];
//	final static long[] hashDP = new long[HSIZEP];
//	static long hashb = 0L;
//	final static long[] hstack = new long[0x800];
//	final static long[] mstack = new long[0x800];
//
//	final static int[][] pv = new int[64][64];
//	final static int[] pvlength = new int[64];
//	final static int[] value = new int[64];
//	static int iter;
//	final static String pieceChar = "*PNK.BRQ";
//	static long searchtime, maxtime, starttime;
//	static boolean sabort, noabort;
//	static boolean ponder = false, pondering = false;
//	static int pon = 0;
//	static int sd = 32;
//
//	static int count, flags, mat, onmove, engine =-1;
//	final static int[] kingpos = new int[2];
//	final static long[] pieceb = new long[8];
//	final static long[] colorb = new long[2];
//	final static StringBuffer irbuf = new StringBuffer();
//	static long BOARD() { return (colorb[0] | colorb[1]); }
//	static long RQU() { return (pieceb[QUEEN] | pieceb[ROOK]); }
//	static long BQU() { return (pieceb[QUEEN] | pieceb[BISHOP]); }
//
//	final static int BKSIZE = 1024;
//	final static int[] bkmove = new int[BKSIZE*32];
//	final static int[] bkflag = new int[BKSIZE];
//	final static int[] bkcount = new int[3];
//
//
//	static long getTime() {
//		return System.currentTimeMillis();
//	}
//
//	static boolean bioskey() {
//		return inStart != inCounter;
//	}
//
//	static void printf(String s) {
//		System.out.print(s);
//	}
//
//	static void errprintf(String s) {
//		System.err.print(s);
//	}
//
//	static void displaypv() {
//		int i;
//		if (pon != 0) { printf("("); displaym(pon); printf(") "); }
//		for (i = 0; i < pvlength[0]; i++) {
//			displaym(pv[0][i]); printf(" ");
//		}
//	}
//
//	static int isDraw(long hp, int nrep) {
//		if (count > 0xFFF) { //fifty > 3
//			int i, c = 0, n = COUNT() - (count >> 10);
//			if (count >= 0x400*100) return 2; //100 plies
//			for (i = COUNT() - 2; i >= n; i--)
//				if (hstack[i] == hp && ++c == nrep) return 1;
//		} else if ((pieceb[PAWN] | RQU()) == 0) { //Check for mating material
//			if (_bitcnt(colorb[0]) <= 2 && _bitcnt(colorb[1]) <= 2) return 3;
//		}
//		return 0;
//	}
//
//	/* In quiesce the moves are ordered just for the value of the captured piece */
//	static int qpick(int[] ml, int mn, int s) {
//		int m;
//		int i, t, pi = 0, vmax = -9999;
//		for (i = s; i < mn; i++) {
//			m = ml[i];
//			t = pval[CAP(m)];
//			if (t > vmax) {
//				vmax = t;
//				pi = i;
//			}
//		}
//		m = ml[pi];
//		if (pi != s) ml[pi] = ml[s];
//		return m;
//	}
//
//	final static int[] killer = new int[128];
//	final static int[] history = new int[0x1000];
//	/* In normal search some basic move ordering heuristics are used */
//	static int spick(int[] ml, int mn, int s, int ply) {
//		int m;
//		int i, pi = 0, vmax = -9999;
//		for (i = s; i < mn; i++) {
//			m = ml[i];
//			if (m == killer[ply]) {
//				pi = i;
//				break;
//			}
//			if (vmax < history[m & 0xFFF]) {
//				vmax = history[m & 0xFFF];
//				pi = i;
//			}
//		}
//		m = ml[pi];
//		if (pi != s) ml[pi] = ml[s];
//		return m;
//	}
//
//	static long nodes;
//	static long qnodes;
//	static int quiesce(long ch, int c, int ply, int alpha, int beta) {
//		int i, w, best = -32000;
//		int cmat = c == 1 ? -mat: mat;
//		if (ply == 63) return eval(c) + cmat;
//
//		if (ch == 0) do {
//			if (cmat - 200 >= beta) return beta;
//			if (cmat + 200 <= alpha) break;
//			best = eval(c) + cmat;
//			if (best > alpha) {
//				alpha = best;
//				if (best >= beta) return beta;
//			}
//		} while(false);
//
//		generate(ch, c, ply, 1, 0);
//		if (ch != 0 && movenum[ply] == 0) return -32000 + ply;
//
//		for (i = 0; i < movenum[ply]; i++) {
//			int m = qpick(movelist[ply], movenum[ply], i);
//			if (ch == 0 && PROM(m) == 0 && pval[PIECE(m)] > pval[CAP(m)] && swap(m) < 0) continue;
//
//			doMove(m, c);
//			qnodes++;
//
//			w = -quiesce(attacked(kingpos[c^1], c^1), c^1, ply+1, -beta, -alpha);
//
//			undoMove(m, c);
//
//			if (w > best) {
//				best = w;
//				if (w > alpha) {
//					alpha = w;
//					if (w >= beta) return beta;
//				}
//			}
//		}
//		return best > -32000 ? best : eval(c) + cmat;
//	}
//
//	static int retPVMove(int c, int ply) {
//        int i;
//        generate(attacked(kingpos[c], c), c, 0, 1, 1);
//        for (i = 0; i < movenum[0]; i++) {
//                int m = movelist[0][i];
//                if (m == pv[0][ply]) return m;
//        }
//        return 0;
//	}
//
//	static boolean inputSearch() {
//        int ex;
//        if (!pondering) return true;
//		irbuf.append(inString.get(inStart));
//		inString.remove(inStart++);
//
//		ex = protV2(irbuf.toString());
//        if (!ponder || ex != 0 || engine != onmove) pondering = false;
//        if (ex == 0) irbuf.setLength(0);
//        if (ex != -1) return !pondering;
//        ex = parseMove(irbuf.toString(), ONMV(pon), pon);
//        if (ex == 0 || ex == -1) return true;
//        irbuf.setLength(0);
//        pon = 0;
//        return false;
//	}
//
//	final static int nullvar[] = new int[] {13, 43, 149, 519, 1809, 6311, 22027};
//	static int nullvariance(int delta) {
//	      int r = 0;
//	      if (delta >= 4) for (r = 1; r <= nullvar.length; r++) if (delta < nullvar[r - 1]) break;
//	      return r;
//	}
//
//	static long HASHP(int c) { return (hashb ^ hashxor[flags | 1024 | (c << 11)]); }
//	static long HASHB(int c, int d) { return ((hashb ^ hashxor[flags | 1024]) ^ hashxor[c | (d << 1) | 2048]); }
//	static int search(long ch, int c, int d, int ply, int alpha, int beta, int pvnode, int isnull) {
//		int i, j, n, w, asave, first, best;
//		int hmove;
//		long hb, hp, he;
//
//		pvlength[ply] = ply;
//		if (ply == 63) return eval(c) + (c != 0 ? -mat: mat);
//		if ((++nodes & CNODES) == 0) {
//			long consumed = getTime() - starttime;
//            if (!pondering && (consumed > maxtime || (consumed > searchtime && !noabort))) sabort = true;
//            if (bioskey()) sabort = inputSearch();
//		}
//		if (sabort) return 0;
//
//		hp = HASHP(c);
//		if (ply != 0 && isDraw(hp, 1) != 0) return 0;
//
//		if (d == 0) return quiesce(ch, c, ply, alpha, beta);
//		hstack[COUNT()] = hp;
//
//		hb = HASHB(c, d);
//		he = hashDB[(int)(hb & HMASKB)];
//		if (((he^hb) & HINVB) == 0) {
//			w = (int)LOW16(he) - 32768;
//			if ((he & 0x10000) != 0) {
//				isnull = 0;
//				if (w <= alpha) return alpha;
//			} else {
//				if (w >= beta) return beta;
//			}
//		} else {
//                w = c != 0 ? -mat : mat;
//        }
//
//		if (pvnode == 0 && ch == 0 && isnull != 0 && d > 1 && bitcnt(colorb[c] & (~pieceb[PAWN]) & (~pinnedPieces(kingpos[c], c^1))) > 2) {
//			int flagstore = flags;
//			int R = (10 + d + nullvariance(w - beta))/4;
//			if (R > d) R = d;
//			flags &= 960;
//			count += 0x401;
//			w = -search(0L, c^1, d-R, ply+1, -beta, -alpha, 0, 0); //Null Move Search
//			flags = flagstore;
//			count -= 0x401;
//			if (!sabort && w >= beta) {
//				hashDB[(int)(hb & HMASKB)] = (hb & HINVB) | (w + 32768);
//				return beta;
//			}
//		}
//
//		hmove = 0;
//		if (ply > 0) {
//			he = hashDP[(int)(hp & HMASKP)];
//			if (((he^hp) & HINVP) == 0) hmove = (int)(he & HMASKP);
//
//			if (d >= 4 && hmove == 0) { // Simple version of Internal Iterative Deepening
//				w = search(ch, c, d-3, ply, alpha, beta, pvnode, 0);
//				he = hashDP[(int)(hp & HMASKP)];
//				if (((he^hp) & HINVP) == 0) hmove = (int)(he & HMASKP);
//			}
//		} else {
//			hmove = retPVMove(c, ply);
//		}
//
//		best = pvnode != 0 ? alpha : -32001;
//		asave = alpha;
//		first = 1;
//		for (n = 1; n <= ((ch != 0L) ? 2 : 3); n++) {
//            if (n == 1) {
//                if (hmove == 0) continue;
//                movenum[ply] = 1;
//            } else if (n == 2) {
//                generate(ch, c, ply, 1, 0);
//            } else {
//                generate(ch, c, ply, 0, 1);
//            }
//            for (i = 0; i < movenum[ply]; i++) {
//                int m;
//                long nch;
//                int ext = 0;
//                if (n == 1) {
//                        m = hmove;
//                } else {
//                        if (n == 2) m = qpick(movelist[ply], movenum[ply], i);
//                        else m = spick(movelist[ply], movenum[ply], i, ply);
//                        if (m == hmove) continue;
//                }
//			doMove(m, c);
//
//			nch = attacked(kingpos[c^1], c^1);
//			if (nch != 0) ext++; // Check Extension
//			else if (d >= 3 && n == 3 && pvnode == 0) { //LMR
//                if (m == killer[ply]); //Don't reduce killers
//                else if (PIECE(m) == PAWN && (pawnfree[c][TO(m)] & pieceb[PAWN] & colorb[c^1]) == 0); //Don't reduce free pawns
//				else ext--;
//			}
//
//			if (first != 0 && pvnode != 0) {
//				w = -search(nch, c^1, d-1+ext, ply+1, -beta, -alpha, 1, 1);
//				if (ply == 0) noabort = (iter > 1 && w < value[iter-1] - 40);
//			} else {
//				w = -search(nch, c^1, d-1+ext, ply+1, -alpha-1, -alpha, 0, 1);
//				if (w > alpha && ext < 0) w = -search(nch, c^1, d-1, ply+1, -alpha-1, -alpha, 0, 1);
//				if (w > alpha && w < beta && pvnode != 0) w = -search(nch, c^1, d-1+ext, ply+1, -beta, -alpha, 1, 1);
//			}
//			undoMove(m, c);
//
//			if (!sabort && w > best) {
//				if (w > alpha) {
//					hashDP[(int)(hp & HMASKP)] = (hp & HINVP) | m;
//					alpha = w;
//				}
//				if (w >= beta) {
//					if (CAP(m) == 0) {
//						killer[ply] = m;
//						history[m & 0xFFF]++;
//					}
//					hashDB[(int)(hb & HMASKB)] = (hb & HINVB) | (w + 32768);
//					return beta;
//				}
//				if (pvnode != 0 && w >= alpha) {
//					pv[ply][ply] = m;
//					for (j = ply +1; j < pvlength[ply +1]; j++) pv[ply][j] = pv[ply +1][j];
//					pvlength[ply] = pvlength[ply +1];
//					if (ply == 0 && iter > 1 && w > value[iter-1] - 20) noabort = false;
//					if (w == 31999 - ply) return w;
//				}
//				best = w;
//			}
//			first = 0;
//		}
//		}
//		if (first != 0) return (ch != 0L) ? -32000+ply : 0;
//        	if (pvnode != 0) {
//            		if (!sabort && asave == alpha) hashDB[(int)(hb & HMASKB)] = (hb & HINVB) | 0x10000 | (asave + 32768);
//        	} else {
//            		if (!sabort && best < beta) hashDB[(int)(hb & HMASKB)] = (hb & HINVB) | 0x10000 | (best + 32768);
//        	}
//		return alpha;
//	}
//
//	static int execMove(int m) {
//		int i, c;
//		doMove(m, onmove);
//		onmove ^= 1;
//		c = onmove;
//		if (book) for (i = 0; i < BKSIZE; i++) {
//			if (bkflag[i] < 2 && (bkmove[i*32 + COUNT() - 1] != m || bkmove[i*32 + COUNT()] == 0)) {
//				bkcount[bkflag[i]]--;
//				bkflag[i] = 2;
//			}
//		}
//		hstack[COUNT()] = HASHP(c);
//		for (i = 0; i < 127; i++) killer[i] = killer[i+1];
//		for (i = 0; i < 0x1000; i++) history[i] = 0;
//		i = generate(attacked(kingpos[c], c), c, 0, 1, 1);
//		if (pondering) return (movenum[0] == 0 ? 7 : 0);
//		if (movenum[0] == 0) {
//			if (i == 0) {
//				printf("1/2-1/2 {Stalemate}\n"); return 4;
//			} else {
//				printf(c == 1 ? "1-0 {White mates}\n" : "0-1 {Black mates}\n"); return 5 + c;
//			}
//		}
//		switch (isDraw(HASHP(c), 2)) {
//			case 1: printf("1/2-1/2 {Draw by Repetition}\n"); return 1;
//			case 2: printf("1/2-1/2 {Draw by Fifty Move Rule}\n"); return 2;
//			case 3: printf("1/2-1/2 {Insufficient material}\n"); return 3;
//		}
//		return 0;
//	}
//
//	static int parseMoveNExec(String s, int c, int[] m) {
//        m[0] = parseMove(s, c, 0);
//        if (m[0] == -1) printf("UNKNOWN COMMAND: " + s + "\n");
//        else if (m[0] == 0) errprintf("Illegal move: " + s + "\n");
//        else return execMove(m[0]);
//        return -1;
//	}
//
//	static void undo() {
//	        int cnt = COUNT() - 1;
//	        onmove ^= 1;
//	        undoMove((int)(mstack[cnt] >> 42L), onmove);
//	}
//
//	static int time = 30000;
//	static int mps = 0;
//	static int base = 5;
//	static int inc = 0;
//	static int st = 0;
//	static boolean post = true;
//
//	static int calc(int sd, int tm) {
//			int i, j, t1 = 0, m2go = 32;
//			long ch = attacked(kingpos[onmove], onmove);
//			eval1 = iter = value[0] = 0;
//			sabort = false;
//			qnodes = nodes = 0L;
//			if (mps > 0) m2go = 1 + mps - ((COUNT()/2) % mps);
//
//			searchtime = (tm*10L)/m2go + inc*1000L;
//			maxtime = inc != 0 ? tm*3L : tm*2L;
//			if (st > 0) maxtime = searchtime = st*1000L;
//
//			starttime = getTime();
//			Random rand = new Random(starttime);
//			if (book) {
//				if (bkcount[onmove] == 0) book = false;
//				else {
//					j = rand.nextInt(bkcount[onmove]);
//					for (i = 0; i < BKSIZE; i++) {
//						if (bkflag[i] == onmove && j == t1++) { pv[0][0] = bkmove[i*32 + COUNT()]; break; }
//					}
//				}
//			}
//			if (!book) for (iter = 1; iter <= sd; iter++) {
//				noabort = false;
//				value[iter] = search(ch, onmove, iter, 0, -32000, 32000, 1, 0);
//				t1 = (int)(getTime() - starttime);
//				if (sabort && pvlength[0] == 0 && (iter--) != 0) break;
//				if (post && pvlength[0] > 0) {
//					System.out.printf("%2d %5d %6d %9d  ", iter, value[iter], t1/10, (int)(nodes + qnodes));
//					displaypv(); printf("\n");
//				}
//				if (!pondering && (iter >= 32000-value[iter] || sabort || t1 > searchtime/2)) break;
//			}
//            pondering = false;
//            if (pon != 0) {
//                undo();
//                pon = 0;
//                return engine != onmove ? 1 : 0;
//            }
//			printf("move "); displaym(pv[0][0]); printf("\n");
//
//			if (post) printf("\nkibitz W: " + value[iter > sd ? sd : iter]
//					+ " Nodes: " + nodes
//					+ " QNodes: " + qnodes
//					+ " Evals: " + eval1
//					+ " cs: " + t1/10
//					+ " knps: "+ (nodes+qnodes)/(t1+1) + "\n");
//			return execMove(pv[0][0]);
//	}
//
//	static int doponder(int c) {
//        pon = retPVMove(c, 1);
//        if (pon != 0) {
//                pondering = true;
//                if (execMove(pon) != 0) {
//                        pondering = false;
//                        undo();
//                        pon = 0;
//                }
//        }
//        return pondering ? 0 : -1;
//	}
//
//	static int protV2(String buf) {
//		if (buf.startsWith("protover")) printf("feature setboard=1 myname=\"OliThink " + VER + "\" colors=0 analyze=0 done=1\n");
//		else if (buf.startsWith("xboard"));
//		else if (buf.startsWith("quit")) return -2;
//		else if (buf.startsWith("new")) return -3;
//		else if (buf.startsWith("remove")) return -4;
//		else if (buf.startsWith("force")) engine = -1;
//		else if (buf.startsWith("go")) engine = pondering ? onmove^1 : onmove;
//		else if (buf.startsWith("setboard")) _parse_fen(buf.substring(9));
//		else if (buf.startsWith("undo")) undo();
//		else if (buf.startsWith("easy")) ponder = false;
//		else if (buf.startsWith("hard")) ponder = true;
//		else if (buf.startsWith("sd")) sd = Integer.parseInt(buf.substring(3));
//		else if (buf.startsWith("time")) time = Integer.parseInt(buf.substring(5));
//		else if (buf.startsWith("level")) {
//			StringTokenizer st = new StringTokenizer(buf.substring(6), " ");
//			mps = Integer.parseInt(st.nextToken());
//			base = Integer.parseInt(st.nextToken());
//			inc = Integer.parseInt(st.nextToken());
//		}
//		else if (buf.startsWith("post")) post = true;
//		else if (buf.startsWith("nopost")) post = false;
//		else if (buf.startsWith("result"));//result 0-1 {Black mates}
//		else if (buf.startsWith("otim"));//otim <optime>
//		else if (buf.startsWith("draw"));//draw offer
//		else if (buf.startsWith("st")) st = Integer.parseInt(buf.substring(3));
//		else if (buf.startsWith("bk"));
//		else if (buf.startsWith("hint"));
//		else if (buf.startsWith("computer"));
//		else if (buf.startsWith("accepted"));//accepted <feature>
//		else if (buf.startsWith("random"));
//		else if (buf.startsWith("rating"));//ICS: rating <myrat> <oprat>
//		else if (buf.startsWith("name"));//ICS: name <opname>
//		else if (buf.startsWith("perft")) {int i; for (i = 1; i <= sd; i++) printf("Depth: " + i + " Nodes: " + perft(onmove, i, 0) + "\n");}
//        else if (buf.startsWith("divide")) perft(onmove, sd, 1);
//        else return -1;
//        return 0;
//	}
//}
