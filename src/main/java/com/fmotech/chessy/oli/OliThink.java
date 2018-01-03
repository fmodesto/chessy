/* OliThink5 Java(c) Oliver Brausch 04.Mar.2012, ob112@web.de, http://home.arcor.de/dreamlike */

package com.fmotech.chessy.oli;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

public class OliThink {
	final static String VER = "5.3.2 Java";
	final static Class<?> otclass = OliThink.class;

	final static int PAWN = 1;
	final static int KNIGHT = 2;
	final static int KING = 3;
	final static int ENP = 4;
	final static int BISHOP = 5;
	final static int ROOK = 6;
	final static int QUEEN = 7;

	final static int CNODES = 0xFFFF;
	final static int pval[] = {0, 100, 290, 0, 100, 310, 500, 950};
	final static int pawnrun[] = {0, 0, 1, 8, 16, 32, 64, 128};

	static int FROM(int x) { return ((x) & 63); }
	static int TO(int x) { return (((x) >> 6) & 63); }
	static int PROM(int x) { return (((x) >> 12) & 7); }
	static int PIECE(int x) { return (((x) >> 15) & 7); }
	static int ONMV(int x) { return (((x) >> 18) & 1); }
	static int CAP(int x) { return (((x) >> 19) & 7); }

	static int _TO(int x) { return ((x) << 6); }
	static int _PROM(int x) { return ((x) << 12); }
	static int _PIECE(int x) { return ((x) << 15); }
	static int _ONMV(int x) { return ((x) << 18); }
	static int _CAP(int x) { return ((x) << 19); }
	static int PREMOVE(int f, int p, int c) { return ((f) | _ONMV(c) | _PIECE(p)); }

	static long RATT1(int f) { return rays[((f) << 7) | key000(BOARD(), f)]; }
	static long RATT2(int f) { return rays[((f) << 7) | key090(BOARD(), f) | 0x2000]; }
	static long BATT3(int f) { return rays[((f) << 7) | key045(BOARD(), f) | 0x4000]; }
	static long BATT4(int f) { return rays[((f) << 7) | key135(BOARD(), f) | 0x6000]; }
	static long RXRAY1(int f) { return rays[((f) << 7) | key000(BOARD(), f) | 0x8000]; }
	static long RXRAY2(int f) { return rays[((f) << 7) | key090(BOARD(), f) | 0xA000]; }
	static long BXRAY3(int f) { return rays[((f) << 7) | key045(BOARD(), f) | 0xC000]; }
	static long BXRAY4(int f) { return rays[((f) << 7) | key135(BOARD(), f) | 0xE000]; }

	static long ROCC1(int f) { return (RATT1(f) & BOARD()); }
	static long ROCC2(int f) { return (RATT2(f) & BOARD()); }
	static long BOCC3(int f) { return (BATT3(f) & BOARD()); }
	static long BOCC4(int f) { return (BATT4(f) & BOARD()); }
	static long RMOVE1(int f) { return (RATT1(f) & (~BOARD())); }
	static long RMOVE2(int f) { return (RATT2(f) & (~BOARD())); }
	static long BMOVE3(int f) { return (BATT3(f) & (~BOARD())); }
	static long BMOVE4(int f) { return (BATT4(f) & (~BOARD())); }
	static long RCAP1(int f, int c) { return (RATT1(f) & colorb[(c)^1]); }
	static long RCAP2(int f, int c) { return (RATT2(f) & colorb[(c)^1]); }
	static long BCAP3(int f, int c) { return (BATT3(f) & colorb[(c)^1]); }
	static long BCAP4(int f, int c) { return (BATT4(f) & colorb[(c)^1]); }
	static long ROCC(int f) { return (ROCC1(f) | ROCC2(f)); }
	static long BOCC(int f) { return (BOCC3(f) | BOCC4(f)); }
	static long RMOVE(int f) { return (RMOVE1(f) | RMOVE2(f)); }
	static long BMOVE(int f) { return (BMOVE3(f) | BMOVE4(f)); }
	static long RCAP(int f, int c) { return (ROCC(f) & colorb[(c)^1]); }
	static long BCAP(int f, int c) { return (BOCC(f) & colorb[(c)^1]); }

	static long SHORTMOVE(long x) { return ((x) & ((x)^BOARD())); }
	static long SHORTOCC(long x) { return ((x) & BOARD()); }
	static long SHORTCAP(long x, int c) { return ((x) & colorb[(c)^1]); }

	static long NMOVE(int x) { return (SHORTMOVE(nmoves[x])); }
	static long KMOVE(int x) { return (SHORTMOVE(kmoves[x])); }
	static long PMOVE(int x, int c) { return (pmoves[(c)][(x)] & (~BOARD())); }
	static long NOCC(int x) { return (SHORTOCC(nmoves[x])); }
	static long KOCC(int x) { return (SHORTOCC(kmoves[x])); }
	static long POCC(int x, int c) { return (pcaps[(c)][(x)] & BOARD()); }
	static long NCAP(int x, int c) { return (SHORTCAP(nmoves[x], (c))); }
	static long KCAP(int x, int c) { return (SHORTCAP(kmoves[x], (c))); }
	static long PCAP(int x, int c) { return (pcaps[(c)][(x)] & colorb[(c)^1]); }
	static long PCA3(int x, int c) { return (pcaps[(c)][(x) | 64] & (colorb[(c)^1] | ((BIT[ENPASS()]) & (c == 1 ? 0xFF0000L : 0xFF0000000000L)))); }
	static long PCA4(int x, int c) { return (pcaps[(c)][(x) | 128] & (colorb[(c)^1] | ((BIT[ENPASS()]) & (c == 1? 0xFF0000L : 0xFF0000000000L)))); }

	static boolean RANK(int x, int y) { return (((x) & 0x38) == (y)); }
	static boolean TEST(int f, long b) { return (BIT[f] & (b)) != 0; }
	static int ENPASS() { return (flags & 63); }
	static int CASTLE() { return (flags & 960); }
	static int COUNT() { return (count & 0x3FF); }

	final static int HSIZEB = 0x200000;
	final static int HMASKB = HSIZEB - 1;
	final static long HINVB = 0xFFFFFFFF00000000L | (0xFFFFFFFFL & ~HMASKB);

	final static int HSIZEP = 0x400000;
	final static int HMASKP = HSIZEP - 1;
	final static long HINVP = 0xFFFFFFFF00000000L | (0xFFFFFFFFL & ~HMASKP);
	final static long[] hashDB = new long[HSIZEB];
	final static long[] hashDP = new long[HSIZEP];
	static long hashb = 0L;
	final static long[] hstack = new long[0x800];
	final static long[] mstack = new long[0x800];

	final static long[] hashxor = new long[4096];
	public final static long[] rays = new long[0x10000];
	final static long[][] pmoves = new long[2][64];
	final static long[][] pcaps = new long[2][192];
	final static long[] nmoves = new long[64];
	final static long[] kmoves = new long[64];
	final static int[] _knight = {-17,-10,6,15,17,10,-6,-15};
	final static int[] _king = {-9,-1,7,8,9,1,-7,-8};
	final static long[] BIT = new long[64];
	final static byte[] LSB = new byte[0x10000];
	final static byte[] BITC = new byte[0x10000] ;      
	final static int[] crevoke = new int[64];
	final static int[] nmobil = new int[64];
	final static int[] kmobil = new int[64];
	final static int[][] pawnprg = new int[2][64];
	final static long[][] pawnfree = new long[2][64];
	final static long[][] pawnfile = new long[2][64];
	final static long[][] pawnhelp = new long[2][64];
	final static int[][] movelist = new int[64][256];
	final static int[] movenum = new int[64];
	final static int[][] pv = new int[64][64];
	final static int[] pvlength = new int[64];
	final static int[] value = new int[64];
	static int iter;
	final static String pieceChar = "*PNK.BRQ";
	static long searchtime, maxtime, starttime;
	static boolean sabort, noabort;
	static boolean ponder = false, pondering = false;
	static int pon = 0;
	static int sd = 32;

	static int count;
	static int flags;
	static int mat;
	public static int onmove;
	static int engine =-1;
	final static int[] kingpos = new int[2];
	final static long[] pieceb = new long[8];
	final static long[] colorb = new long[2];
	final static StringBuffer irbuf = new StringBuffer();
	static long BOARD() { return (colorb[0] | colorb[1]); }
	static long RQU() { return (pieceb[QUEEN] | pieceb[ROOK]); }
	static long BQU() { return (pieceb[QUEEN] | pieceb[BISHOP]); }

	static long getLowestBit(long bb) {
		return bb & (-(long)bb);
	}

	static int _getpiece(char s, int[] c) {
		int i;
		for (i = 1; i < 8; i++) 
			if (pieceChar.charAt(i) == s) { c[0] = 0; return i; } 
			else if (pieceChar.charAt(i) == s-32) { c[0] = 1; return i; }
		return 0;
	}

	static boolean book;
	public static void _parse_fen(String fen) {
		char s, mv = 'w';
		String pos = "", cas = "", enps = "";
		int c, i, halfm = 0, fullm = 1, col = 0, row = 7;
		for (i = 0; i < 8; i++) pieceb[i] = 0L;
		colorb[0] = colorb[1] = hashb = 0L;
		mat = i = c = 0;
		book = false;
		StringTokenizer st = new StringTokenizer(fen, " ");
		if (st.hasMoreTokens()) pos = st.nextToken();
		if (st.hasMoreTokens()) mv = st.nextToken().charAt(0);
		if (st.hasMoreTokens()) cas = st.nextToken();
		if (st.hasMoreTokens()) enps = st.nextToken();
		try {
			if (st.hasMoreTokens()) halfm = Integer.parseInt(st.nextToken());
			if (st.hasMoreTokens()) fullm = Integer.parseInt(st.nextToken());
		} catch (NumberFormatException nfe) { }

		for (i = 1; i <= pos.length(); i++) {
			s = pos.charAt(i - 1);
			if (s == '/') {
				row--;
				col = 0;
			} else if (s >= '1' && s <= '8') {
				col += s - '0';
			} else {
				int[] cp = new int[]{c};
				int p = _getpiece(s, cp);
				c = cp[0];
				if (p == KING) kingpos[c] = row*8 + col;
				else mat += c == 1 ? -pval[p] : pval[p];
				hashb ^= hashxor[col | row << 3 | i << 6 | (c == 1 ? 512 : 0)];
				pieceb[p] |= BIT[row*8 + col];
				colorb[c] |= BIT[row*8 + (col++)];
			}
		}
		onmove = mv == 'b' ? 1 : 0;
		flags = i = 0;
		for (i = 0; i < cas.length(); i++) {
			s = cas.charAt(i);
			if (s == 'K') flags |= BIT[6];
			if (s == 'k') flags |= BIT[7];
			if (s == 'Q') flags |= BIT[8];
			if (s == 'q') flags |= BIT[9];
		}
		if (enps.charAt(0) >= 'a' && enps.charAt(0) <= 'h' && enps.charAt(1) >= '1' && enps.charAt(1) <= '8') flags |= 8*(enps.charAt(1) - '1') + enps.charAt(0) - 'a'; 
		count = (fullm - 1)*2 + onmove + (halfm << 10);
		for (i = 0; i < COUNT(); i++) hstack[i] = 0L;
	}

	static String sfen = "rnbqkbnr/pppppppp/////PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	final static int BKSIZE = 1024;
	final static int[] bkmove = new int[BKSIZE*32];
	final static int[] bkflag = new int[BKSIZE];
	final static int[] bkcount = new int[3];

	static void _readbook(String bk) {
		String s0, s1, s2;
		int k, n = 0;
		bkcount[0] = bkcount[1] = 0;
		for (k = 0; k < BKSIZE; k++) bkflag[k] = 2;
		try {
			FileReader fr = new FileReader(bk);
			BufferedReader bf = new BufferedReader(fr);
			String buf = null;
			while ((buf = bf.readLine()) != null) {
				if (buf.length() == 0) continue;
				if (buf.charAt(0) == '[') {
					StringTokenizer st = new StringTokenizer(buf, " ");
					s1 = st.nextToken();
					s2 = st.nextToken();
					if (s2.startsWith("\"OliThink")) bkflag[n] = s1.startsWith("[Black") ? 0 : 1;
				} else if (buf.charAt(0) == '1') {
					int i = 0, j = 0;
					_parse_fen(sfen);
					for (;;) {
						String bufi = buf.substring(i);
						StringTokenizer st = new StringTokenizer(bufi, " ");
						s0 = st.nextToken();
						if (s0.charAt(0) < '1' || s0.charAt(0) > '9') break;
						if (!st.hasMoreTokens()) break;
						s2 = st.nextToken();
						i += s0.length() + s2.length() + 2;
						int n1 = s0.indexOf('.');
						if (n1 < 0) break;
						s1 = s0.substring(n1 + 1);
						int[] ip = new int[1];
						parseMoveNExec(s1, 0, ip);
						bkmove[n*32+ (j++)] = ip[0];
						if (s2.charAt(0) == '*') break;
						parseMoveNExec(s2, 1, ip);
						bkmove[n*32+ (j++)] = ip[0];
						if (j > 30) break;
					}
					bkmove[n*32 + j] = 0;
					if (j != 0) bkcount[bkflag[n]]++;
					if (++n == BKSIZE) break;
				} 				
			}
			bf.close();
			fr.close();
		} catch (IOException e) {
		} catch (AccessControlException e) {
			System.out.println("NO FILEPERMISSION FOR BOOK");
		}

		_parse_fen(sfen);
		if (bkcount[0] > 0 || bkcount[1] > 0) book = true;
		engine = 1;
		sd = 32;
	}

	static int LOW16(long x) { return (int)((x) & 0xFFFF); }
	static int LOW32(long x) { return (int)((x) & 0xFFFFFFFFL); }
	static long L32(long x) { return ((x) & 0xFFFFFFFFL); }
	static long r_x = 30903, r_y = 30903, r_z = 30903, r_w = 30903, r_carry = 0;
	static long _rand_32() {
		r_x = L32(r_x * 69069 + 1);
		r_y ^= L32(r_y << 13);
		r_y ^= L32(r_y >> 17);
		r_y ^= L32(r_y << 5);
		r_y = L32(r_y);
		long t = L32((r_w << 1)) + r_z + r_carry;
		r_carry = (L32(r_z >> 2) + L32(r_w >> 3) + L32(r_carry >> 2)) >> 30;
		r_z = r_w;
		r_w = L32(t);
		return L32(r_x + r_y + r_w);
	}

	static long _rand_64() { long c = _rand_32(); return _rand_32() | (c << 32); }

	static long getTime() {
		return System.currentTimeMillis();
	}

	static byte getLsb(long bm) {
		return (byte) Long.numberOfTrailingZeros(bm);
	}

	static byte _slow_lsb(long bm) {
		int k = -1;
		while (bm != 0) { k++; if ((bm & 1) != 0) break; bm >>= 1; }
		return (byte)k;
	}

	static byte _bitcnt(long bit) {
		byte c = 0;
		while (bit != 0) { bit &= (bit - 1); c++; }
		return c;
	}

	static byte bitcnt (long n) {    
	     return (byte) Long.bitCount(n);
	}

	static int identPiece(int f) {
		if (TEST(f, pieceb[PAWN])) return PAWN;
		if (TEST(f, pieceb[KNIGHT])) return KNIGHT;
		if (TEST(f, pieceb[BISHOP])) return BISHOP;
		if (TEST(f, pieceb[ROOK])) return ROOK;
		if (TEST(f, pieceb[QUEEN])) return QUEEN;
		if (TEST(f, pieceb[KING])) return KING;
		return ENP;
	}

	final static long[] bmask45 = new long[64];
	final static long[] bmask135 = new long[64];
	static int key000(long b, int f) {
		return (int) ((b >> (f & 56)) & 0x7E);
	}

	static int key090(long b, int f) {
        long _b = (b >> (f&7)) & 0x0101010101010101L;
        _b = _b * 0x0080402010080400L;
        return (int)((_b >> 57) & 0x7F);
	}

	static int keyDiag(long _b) {
        _b = _b * 0x0202020202020202L;
        return (int)((_b >> 57) & 0x7F);
	}

	static int key045(long b, int f) {
	   return keyDiag(b & bmask45[f]);
	}

	static int key135(long b, int f) {
	   return keyDiag(b & bmask135[f]);
	}

	static boolean DUALATT(int x, int y, int c) { return (battacked(x, c) || battacked(y, c)); }
	static boolean battacked(int f, int c) {
		if ((PCAP(f, c) & pieceb[PAWN]) != 0) return true;
		if ((NCAP(f, c) & pieceb[KNIGHT]) != 0) return true;
		if ((KCAP(f, c) & pieceb[KING]) != 0) return true;
		if ((RCAP1(f, c) & RQU()) != 0) return true; 
		if ((RCAP2(f, c) & RQU()) != 0) return true; 
		if ((BCAP3(f, c) & BQU()) != 0) return true;
		if ((BCAP4(f, c) & BQU()) != 0) return true;
		return false;
	}

	static long reach(int f, int c) {
		return (NCAP(f, c) & pieceb[KNIGHT])
			| (RCAP1(f, c) & RQU())
			| (RCAP2(f, c) & RQU())
			| (BCAP3(f, c) & BQU())
			| (BCAP4(f, c) & BQU());
	}

	static long  attacked(int f, int c) {
		return (PCAP(f, c) & pieceb[PAWN]) | reach(f, c);
	}

	static void _init_pawns(long[] moves, long[] caps, long[] freep, long[] filep, long[] helpp, int c) {
		int i, j;
		for (i = 0; i < 64; i++) {
			int rank = i/8;
			int file = i&7;
			int m = i + (c == 1 ? -8 : 8);
			pawnprg[c][i] = pawnrun[c == 1 ? 7-rank : rank];
			for (j = 0; j < 64; j++) {
				int jrank = j/8;
				int jfile = j&7;
				int dfile = (jfile - file)*(jfile - file);
				if (dfile > 1) continue;
				if ((c == 1 && jrank < rank) || (c == 0 && jrank > rank)) {//The not touched half of the pawn
					if (dfile == 0) filep[i] |= BIT[j];
					freep[i] |= BIT[j];
				} else if (dfile != 0 && (jrank - rank)*(jrank - rank) <= 1) {
					helpp[i] |= BIT[j];
				}
			}
			if (m < 0 || m > 63) continue;
			moves[i] |= BIT[m];
			if (file > 0) {
				m = i + (c == 1 ? -9 : 7);
				if (m < 0 || m > 63) continue;
				caps[i] |= BIT[m];
				caps[i + 64*(2 - c)] |= BIT[m];
			}
			if (file < 7) {
				m = i + (c == 1 ? -7 : 9);
				if (m < 0 || m > 63) continue;
				caps[i] |= BIT[m];
				caps[i + 64*(c + 1)] |= BIT[m];
			}
		}
	}

	static void _init_shorts(long[] moves, int[] m) {
		int i, j, n;
		for (i = 0; i < 64; i++) {
			for (j = 0; j < 8; j++) {
				n = i + m[j];
				if (n < 64 && n >= 0 && ((n & 7)-(i & 7))*((n & 7)-(i & 7)) <= 4) {
					moves[i] |= BIT[n];
				}
			}
		}
	}

	static long _occ_free_board(int bc, int del, long free) {
		long low, perm = free;
		int i;
		for (i = 0; i < bc; i++) {
			low = getLowestBit(free);
			free &= (~low);
			if (!TEST(i, del)) perm &= (~low);
		}
		return perm;
	}
	
	static void _init_rays(int off, Class<?> c, String srayf, String skey)  throws Exception {
		int i, f, iperm, bc, index; 
		long board, mmask, occ, move, xray;
		Method rayFunc = c.getDeclaredMethod(srayf, int.class, long.class, int.class);
		Method key = c.getDeclaredMethod(skey, long.class, int.class);
		for (f = 0; f < 64; f++) {
			mmask = (Long) rayFunc.invoke(c, f, 0L, 0) | BIT[f];
			iperm = 1 << (bc = bitcnt(mmask));
			for (i = 0; i < iperm; i++) {
				board = _occ_free_board(bc, i, mmask);
				move = (Long) rayFunc.invoke(c, f, board, 1);
				occ = (Long) rayFunc.invoke(c, f, board, 2);
				xray = (Long) rayFunc.invoke(c, f, board, 3);
				index = (Integer) key.invoke(c, board, f);
				rays[(f << 7) + index + off] = occ | move;
				rays[(f << 7) + index + 0x8000 + off] = xray;
			}
		}
	}
	
	static long _rook0(int f, long board, int t) {
		long free = 0L, occ = 0L, xray = 0L;
		int i, b;
		for (b = 0, i = f+1; i < 64 && i%8 != 0; i++) {
			if (TEST(i, board)) { if (b != 0) { xray |= BIT[i]; break; } else { occ |= BIT[i]; b = 1; }} 
			if (b == 0) free |= BIT[i];
		}
		for (b = 0, i = f-1; i >= 0 && i%8 != 7; i--) {
			if (TEST(i, board)) { if (b != 0) { xray |= BIT[i]; break; } else { occ |= BIT[i]; b = 1; }} 
			if (b == 0) free |= BIT[i];
		}
		return (t < 2) ? free : (t == 2 ? occ : xray);
	}

	static long _rook90(int f, long board, int t) {
		long free = 0L, occ = 0L, xray = 0L;
		int i, b;
		for (b = 0, i = f-8; i >= 0; i-=8) {
			if (TEST(i, board)) { if (b != 0) { xray |= BIT[i]; break; } else { occ |= BIT[i]; b = 1; }} 
			if (b == 0) free |= BIT[i];
		}
		for (b = 0, i = f+8; i < 64; i+=8) {
			if (TEST(i, board)) { if (b != 0) { xray |= BIT[i]; break; } else { occ |= BIT[i]; b = 1; }} 
			if (b == 0) free |= BIT[i];
		}
		return (t < 2) ? free : (t == 2 ? occ : xray);
	}

	static long _bishop45(int f, long board, int t) {
		long free = 0L, occ = 0L, xray = 0L;
		int i, b;
		for (b = 0, i = f+9; i < 64 && (i%8 != 0); i+=9) {
			if (TEST(i, board)) { if (b != 0) { xray |= BIT[i]; break; } else { occ |= BIT[i]; b = 1; }} 
			if (b == 0) free |= BIT[i];
		}
		for (b = 0, i = f-9; i >= 0 && (i%8 != 7); i-=9) {
			if (TEST(i, board)) { if (b != 0) { xray |= BIT[i]; break; } else { occ |= BIT[i]; b = 1; }} 
			if (b == 0) free |= BIT[i];
		}
		return (t < 2) ? free : (t == 2 ? occ : xray);
	}

	static long _bishop135(int f, long board, int t) {
		long free = 0L, occ = 0L, xray = 0L;
		int i, b;
		for (b = 0, i = f-7; i >= 0 && (i%8 != 0); i-=7) {
			if (TEST(i, board)) { if (b != 0) { xray |= BIT[i]; break; } else { occ |= BIT[i]; b = 1; }} 
			if (b == 0) free |= BIT[i];
		}
		for (b = 0, i = f+7; i < 64 && (i%8 != 7); i+=7) {
			if (TEST(i, board)) { if (b != 0) { xray |= BIT[i]; break; } else { occ |= BIT[i]; b = 1; }} 
			if (b == 0) free |= BIT[i];
		}
		return (t < 2) ? free : (t == 2 ? occ : xray);
	}
	
	static String displaym(int m) {
		String move = String.valueOf((char)('a' + FROM(m) % 8))
				+ String.valueOf((char)('1' + FROM(m) / 8))
				+ String.valueOf((char)('a' + TO(m) % 8))
				+ String.valueOf((char)('1' + TO(m) / 8))
				+ (PROM(m) != 0 ? String.valueOf((char)(pieceChar.charAt(PROM(m))+32)) : "");
		printf(move);
		return move;
	}

	static boolean bioskey() {
		return inStart != inCounter;
	}

	static class ReadThread extends Thread {
		boolean stop = false;
		public void run() {
			while (!this.stop) {
				readln();
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}
		}
	}
	
	static void printf(String s) {
		System.out.print(s);
	}

	static void errprintf(String s) {
		System.err.print(s);
	}

	static void displaypv() {
		int i;
		if (pon != 0) { printf("("); displaym(pon); printf(") "); }
		for (i = 0; i < pvlength[0]; i++) {
			displaym(pv[0][i]); printf(" ");
		}
	}

	static int isDraw(long hp, int nrep) {
		if (count > 0xFFF) { //fifty > 3
			int i, c = 0, n = COUNT() - (count >> 10);
			if (count >= 0x400*100) return 2; //100 plies
			for (i = COUNT() - 2; i >= n; i--) 
				if (hstack[i] == hp && ++c == nrep) return 1; 
		} else if ((pieceb[PAWN] | RQU()) == 0) { //Check for mating material
			if (_bitcnt(colorb[0]) <= 2 && _bitcnt(colorb[1]) <= 2) return 3;
		}
		return 0;
	}

	static long pinnedPieces(int f, int oc) {
		long pin = 0L;
		long b = ((RXRAY1(f) | RXRAY2(f)) & colorb[oc]) & RQU();
		while (b != 0) {
			int t = getLsb(b);
			b ^= BIT[t];
			pin |= RCAP(t, oc) & ROCC(f);
		}
		b = ((BXRAY3(f) | BXRAY4(f)) & colorb[oc]) & BQU();
		while (b != 0) {
			int t = getLsb(b);
			b ^= BIT[t];
			pin |= BCAP(t, oc) & BOCC(f);
		}
		return pin;
	}

	static byte getDir(int f, int t) {
		if (((f ^ t) & 56) == 0) return 8;
		if (((f ^ t) & 7) == 0) return 16;
		return (byte)(((f - t) % 7) != 0 ? 32 : 64);
	}

	/* move is both makeMove and unmakeMove, only for unmakeMove the globalflags have to be restored (counter, castle, enpass...) */
	static void move(int m, int c) {
		int f = FROM(m);
		int t = TO(m);
		int p = PIECE(m);
		int a = CAP(m);

		colorb[c] ^= BIT[f];
		pieceb[p] ^= BIT[f];

		colorb[c] ^= BIT[t];
		pieceb[p] ^= BIT[t];
		hashb ^= hashxor[(f) | (p) << 6 | (c) << 9];
		hashb ^= hashxor[(t) | (p) << 6 | (c) << 9];

		flags &= 960;
		count += 0x401;
		if (a != 0) {
			if (a == ENP) { // Enpassant Capture
				t = (t&7) | (f&56);
				a = PAWN;
			} else if (a == ROOK && CASTLE() != 0) { //Revoke castling rights.
				flags &= crevoke[t];
			}
			pieceb[a] ^= BIT[t];
			colorb[c^1] ^= BIT[t];
			hashb ^= hashxor[(t) | (a) << 6 | (c^1) << 9];
			count &= 0x3FF; //Reset Fifty Counter
			mat += c == 1 ? -pval[a] : +pval[a];
		}
		if (p == PAWN) {
			if (((f^t)&8) == 0) flags |= f^24; //Enpassant
			else if ((t&56) == 0 || (t&56) == 56) {
				pieceb[PAWN] ^= BIT[t];
				pieceb[PROM(m)] ^= BIT[t];
				hashb ^= hashxor[(t) | (PAWN) << 6 | (c) << 9];
				hashb ^= hashxor[(t) | (PROM(m)) << 6 | (c) << 9];
				mat += c == 1 ? pval[PAWN] - pval[PROM(m)] : -pval[PAWN] + pval[PROM(m)];
			}
			count &= 0x3FF; //Reset Fifty Counter
		} else if (p == KING) {
			if (kingpos[c] == f) kingpos[c] = t; else kingpos[c] = f;
			flags &= ~(320 << c); // Lose castling rights
			if (((f^t)&3) == 2) { // Castle
				if (t == 6) { f = 7; t = 5; }
				else if (t == 2) { f = 0; t = 3; }
				else if (t == 62) { f = 63; t = 61; }
				else { f = 56; t = 59; }
				colorb[c] ^= BIT[f];
				pieceb[ROOK] ^= BIT[f];
				colorb[c] ^= BIT[t];
				pieceb[ROOK] ^= BIT[t];
				hashb ^= hashxor[(f) | (ROOK) << 6 | (c) << 9];
				hashb ^= hashxor[(t) | (ROOK) << 6 | (c) << 9];
			}
		} else if (p == ROOK && CASTLE() != 0) {
			flags &= crevoke[f];
		}
	}

	static void doMove(int m, int c) {
        mstack[COUNT()] = count | (flags << 17) | (((long)(mat + 0x4000)) << 27) | (((long)m) << 42);
        move(m, c);
	}

	static void undoMove(int m, int c) {
	        long u = mstack[COUNT() - 1];
	        move(m, c);
	        count = (int)(u & 0x1FFFF);
	        flags = (int)((u >> 17L) & 0x3FF);
	        mat = (int)(((u >> 27L) & 0x7FFF) - 0x4000);
	}

	static void regMoves(int m, long bt, int[] mlist, int[] mn, int cap) {
		while (bt != 0) {
			int t = getLsb(bt);
			bt ^= BIT[t];
			mlist[mn[0]++] = m | _TO(t) | (cap != 0 ? _CAP(identPiece(t)) : 0);
		}
	}
	
	static void regMovesCaps(int m, long bc, long bm, int[]  mlist, int[] mn) {
        regMoves(m, bc, mlist, mn, 1);
        regMoves(m, bm, mlist, mn, 0);
	}

	static void regPromotions(int f, int c, long bt, int[] mlist, int[] mn, int cap, int queen) {
		while (bt != 0) {
			int t = getLsb(bt);
			bt ^= BIT[t];
			int m = f | _ONMV(c) | _PIECE(PAWN) | _TO(t) | (cap != 0 ? _CAP(identPiece(t)) : 0);
			if (queen != 0) mlist[mn[0]++] = m | _PROM(QUEEN);
			mlist[mn[0]++] = m | _PROM(KNIGHT);
			mlist[mn[0]++] = m | _PROM(ROOK);
			mlist[mn[0]++] = m | _PROM(BISHOP);
		}
	}

	static void regKings(int m, long bt, int[] mlist, int[] mn, int c, int cap) {
		while (bt != 0) {
			int t = getLsb(bt);
			bt ^= BIT[t];
			if (battacked(t, c)) continue;
			mlist[mn[0]++] = m | _TO(t) | (cap != 0 ? _CAP(identPiece(t)) : 0);
		}
	}

	static int generateCheckEsc(long ch, long apin, int c, int k, int[] ml, int[] mn) {
		long cc, fl;
		int d, bf = _bitcnt(ch);
		colorb[c] ^= BIT[k];
		regKings(PREMOVE(k, KING, c), KCAP(k, c), ml, mn, c, 1);
		regKings(PREMOVE(k, KING, c), KMOVE(k), ml, mn, c, 0);
		colorb[c] ^= BIT[k];
		if (bf > 1) return bf; //Doublecheck
		bf = getLsb(ch);

		cc = attacked(bf, c^1) & apin;  //Can we capture the checker?
		while (cc != 0) {
			int cf = getLsb(cc);
			cc ^= BIT[cf];
			int p = identPiece(cf);
			if (p == PAWN && RANK(cf, c != 0 ? 0x08 : 0x30)) {
				regPromotions(cf, c, ch, ml, mn, 1, 1);
			} else {
				regMovesCaps(PREMOVE(cf, p, c), ch, 0L, ml, mn);
			}
		}
		if (ENPASS() != 0 && (ch & pieceb[PAWN]) != 0) { //Enpassant capture of attacking Pawn
			cc = PCAP(ENPASS(), c^1) & pieceb[PAWN] & apin;
			while (cc != 0) {
				int cf = getLsb(cc);
				cc ^= BIT[cf];
				regMovesCaps(PREMOVE(cf, PAWN, c), BIT[ENPASS()], 0L, ml, mn);
			}
		}
		if ((ch & (nmoves[k] | kmoves[k])) != 0) return 1; //We can't move anything between!

		d = getDir(bf, k);
		if ((d & 8) != 0) fl = RMOVE1(bf) & RMOVE1(k);
		else if ((d & 16) != 0) fl = RMOVE2(bf) & RMOVE2(k);
		else if ((d & 32) != 0) fl = BMOVE3(bf) & BMOVE3(k);
		else fl = BMOVE4(bf) & BMOVE4(k);

		while (fl != 0) {
			int f = getLsb(fl);
			fl ^= BIT[f];
			cc = reach(f, c^1) & apin;
			while (cc != 0) {
				int cf = getLsb(cc);
				cc ^= BIT[cf];
				int p = identPiece(cf);
				regMovesCaps(PREMOVE(cf, p, c), 0L, BIT[f], ml, mn);
			}
			bf = c != 0 ? f+8 : f-8;
			if (bf < 0 || bf > 63) continue;
			if ((BIT[bf] & pieceb[PAWN] & colorb[c] & apin) != 0) {
				if (RANK(bf, c != 0 ? 0x08 : 0x30))
					regPromotions(bf, c, BIT[f], ml, mn, 0, 1);
				else
					regMovesCaps(PREMOVE(bf, PAWN, c), 0L, BIT[f], ml, mn);
			}
			if (RANK(f, c != 0 ? 0x20 : 0x18) && (BOARD() & BIT[bf]) == 0 && (BIT[c != 0 ? f+16 : f-16] & pieceb[PAWN] & colorb[c] & apin) != 0)
				regMovesCaps(PREMOVE(c != 0 ? f+16 : f-16, PAWN, c), 0L, BIT[f], ml, mn);
		}
		return 1;
	}

	static int generateNonCaps(long ch, int c, int f, long pin, int[] ml, int[] mn) {
		int t;
		long m, b, cb = colorb[c] & (~pin);

		regKings(PREMOVE(f, KING, c), KMOVE(f), ml, mn, c, 0);

		b = pieceb[PAWN] & cb;
		while (b != 0) {
			f = getLsb(b);
			b ^= BIT[f];
			m = PMOVE(f, c);
			if (m != 0 && RANK(f, c != 0 ? 0x30 : 0x08)) m |= PMOVE(c != 0 ? f-8 : f+8, c);
			if (RANK(f, c != 0 ? 0x08 : 0x30)) {
				long a = PCAP(f, c);
				regPromotions(f, c, m, ml, mn, 0, 0);
                if (a != 0L) regPromotions(f, c, a, ml, mn, 1, 0);
			} else {
				regMoves(PREMOVE(f, PAWN, c), m, ml, mn, 0);
			}
		}

		b = pin & pieceb[PAWN]; 
		while (b != 0) {
			f = getLsb(b);
			b ^= BIT[f];
			t = getDir(f, kingpos[c]);
			if ((t & 8) != 0) continue;
			m = 0L;
			if ((t & 16) != 0) {
				m = PMOVE(f, c);         
				if (m != 0 && RANK(f, c != 0 ? 0x30 : 0x08)) m |= PMOVE(c != 0 ? f-8 : f+8, c);
			} 
			if (RANK(f, c != 0 ? 0x08 : 0x30)) {
				long a = (t & 32) != 0 ? PCA3(f, c) : ((t & 64) != 0 ? PCA4(f, c) : 0L);
				regPromotions(f, c, m, ml, mn, 0, 0);
				if (a != 0L) regPromotions(f, c, a, ml, mn, 1, 0);
			} else {
				regMoves(PREMOVE(f, PAWN, c), m, ml, mn, 0);
			}
		}

		b = pieceb[KNIGHT] & cb;
		while (b != 0) {
			f = getLsb(b);
			b ^= BIT[f];
			regMoves(PREMOVE(f, KNIGHT, c), NMOVE(f), ml, mn, 0);
		}

		b = pieceb[ROOK] & cb;
		while (b != 0) {
			f = getLsb(b);
			b ^= BIT[f];
			regMoves(PREMOVE(f, ROOK, c), RMOVE(f), ml, mn, 0);
			if (CASTLE() != 0 && ch == 0) {
				if (c != 0) {
					if ((flags & 128) != 0 && (f == 63) && (RMOVE1(63) & BIT[61]) != 0)
						if (!DUALATT(61, 62, c)) regMoves(PREMOVE(60, KING, c), BIT[62], ml, mn, 0);
					if ((flags & 512) != 0 && (f == 56) && (RMOVE1(56) & BIT[59]) != 0)
						if (!DUALATT(59, 58, c)) regMoves(PREMOVE(60, KING, c), BIT[58], ml, mn, 0);
				} else {
					if ((flags & 64) != 0 && (f == 7) && (RMOVE1(7) & BIT[5]) != 0)
						if (!DUALATT(5, 6, c)) regMoves(PREMOVE(4, KING, c), BIT[6], ml, mn, 0);
					if ((flags & 256) != 0 && (f == 0) && (RMOVE1(0) & BIT[3]) != 0)
						if (!DUALATT(3, 2, c)) regMoves(PREMOVE(4, KING, c), BIT[2], ml, mn, 0);
				}
			}
		}

		b = pieceb[BISHOP] & cb;
		while (b != 0) {
			f = getLsb(b);
			b ^= BIT[f];
			regMoves(PREMOVE(f, BISHOP, c), BMOVE(f), ml, mn, 0);
		}

		b = pieceb[QUEEN] & cb;
		while (b != 0) {
			f = getLsb(b);
			b ^= BIT[f];
			regMoves(PREMOVE(f, QUEEN, c), RMOVE(f) | BMOVE(f), ml, mn, 0);
		}

		b = pin & (pieceb[ROOK] | pieceb[BISHOP] | pieceb[QUEEN]); 
		while (b != 0) {
			int p;
			f = getLsb(b);
			b ^= BIT[f];
			p = identPiece(f);
			t = p | getDir(f, kingpos[c]);
			if ((t & 10) == 10) regMoves(PREMOVE(f, p, c), RMOVE1(f), ml, mn, 0);
			if ((t & 18) == 18) regMoves(PREMOVE(f, p, c), RMOVE2(f), ml, mn, 0);
			if ((t & 33) == 33) regMoves(PREMOVE(f, p, c), BMOVE3(f), ml, mn, 0);
			if ((t & 65) == 65) regMoves(PREMOVE(f, p, c), BMOVE4(f), ml, mn, 0);
		}
		return 0;
	}

	static int generateCaps(long ch, int c, int f, long pin, int[] ml, int[] mn) {
		int t;
		long m, b, a, cb = colorb[c] & (~pin);

		regKings(PREMOVE(f, KING, c), KCAP(f, c), ml, mn, c, 1);

		b = pieceb[PAWN] & cb;
		while (b != 0) {
			f = getLsb(b);
			b ^= BIT[f];
			a = PCAP(f, c);
			if (RANK(f, c != 0 ? 0x08 : 0x30)) {
				regMovesCaps(PREMOVE(f, PAWN, c) | _PROM(QUEEN), a, PMOVE(f, c), ml, mn);
			} else {
				if (ENPASS() != 0 && (BIT[ENPASS()] & pcaps[(c)][(f)]) != 0) {
					long hh;
					int clbd = ENPASS()^8;
					colorb[c^1] ^= BIT[clbd];
					hh = ROCC1(f);
					if ((hh & BIT[kingpos[c]]) == 0 || (hh & colorb[c^1] & RQU()) == 0) {
						a = a | BIT[ENPASS()];
					}
					colorb[c^1] ^= BIT[clbd];
				}
				regMoves(PREMOVE(f, PAWN, c), a, ml, mn, 1);
			}
		}

		b = pin & pieceb[PAWN]; 
		while (b != 0) {
			f = getLsb(b);
			b ^= BIT[f];
			t = getDir(f, kingpos[c]);
			if ((t & 8) != 0) continue;
			m = a = 0L;
			if ((t & 16) != 0) {
				m = PMOVE(f, c);         
			} else if ((t & 32) != 0) {
				a = PCA3(f, c);
			} else {
				a = PCA4(f, c);
			}
			if (RANK(f, c != 0 ? 0x08 : 0x30)) {
				regMovesCaps(PREMOVE(f, PAWN, c) | _PROM(QUEEN), a, m, ml, mn);
			} else {
				regMoves(PREMOVE(f, PAWN, c), a, ml, mn, 1);
			}
		}

		b = pieceb[KNIGHT] & cb;
		while (b != 0) {
			f = getLsb(b);
			b ^= BIT[f];
			regMoves(PREMOVE(f, KNIGHT, c), NCAP(f, c), ml, mn, 1);
		}

		b = pieceb[BISHOP] & cb;
		while (b != 0) {
			f = getLsb(b);
			b ^= BIT[f];
			regMoves(PREMOVE(f, BISHOP, c), BCAP(f, c), ml, mn, 1);
		}

		b = pieceb[ROOK] & cb;
		while (b != 0) {
			f = getLsb(b);
			b ^= BIT[f];
			regMoves(PREMOVE(f, ROOK, c), RCAP(f, c), ml, mn, 1);
		}

		b = pieceb[QUEEN] & cb;
		while (b != 0) {
			f = getLsb(b);
			b ^= BIT[f];
			regMoves(PREMOVE(f, QUEEN, c), RCAP(f, c) | BCAP(f,c), ml, mn, 1);
		}

		b = pin & (pieceb[ROOK] | pieceb[BISHOP] | pieceb[QUEEN]); 
		while (b != 0) {
			int p;
			f = getLsb(b);
			b ^= BIT[f];
			p = identPiece(f);
			t = p | getDir(f, kingpos[c]);
			if ((t & 10) == 10) regMoves(PREMOVE(f, p, c), RCAP1(f, c), ml, mn, 1);
			if ((t & 18) == 18) regMoves(PREMOVE(f, p, c), RCAP2(f, c), ml, mn, 1);
			if ((t & 33) == 33) regMoves(PREMOVE(f, p, c), BCAP3(f, c), ml, mn, 1);
			if ((t & 65) == 65) regMoves(PREMOVE(f, p, c), BCAP4(f, c), ml, mn, 1);
		}
		return 0;
	}
	
	static int generate(long ch, int c, int ply, int cap, int noncap) {
        int f = kingpos[c];
        long pin = pinnedPieces(f, c^1);
		int[] ml = movelist[ply];
		int[] mn = new int[]{0};

        if (ch != 0L) {
        	int r = generateCheckEsc(ch, ~pin, c, f, ml, mn);
    		movenum[ply] = mn[0];
    		return r;
        }
        if (cap != 0) generateCaps(ch, c, f, pin, ml, mn);
        if (noncap != 0) generateNonCaps(ch, c, f, pin, ml, mn);
		movenum[ply] = mn[0];
        return 0;
}


	static int swap(int m) //SEE Stuff
	{
	  int[] s_list = new int[32];
	  int f = FROM(m), t = TO(m), onmv = ONMV(m);
	  int a_piece = pval[CAP(m)], piece = PIECE(m), c = onmv^1, nc = 1;
	  long attacks, temp = 0, colstore0 = colorb[0], colstore1 = colorb[1];

	  attacks = attacked(t, 0) | attacked(t, 1);
	  s_list[0] = a_piece;
	  a_piece = pval[piece];
	  colorb[onmv] ^= BIT[f];
	  if ((piece & 4) != 0 || piece == 1) {
		int d = getDir(f, t);
	    if (d == 32 || d == 64) attacks |= BOCC(t) & BQU();
		if (d == 8 || d == 16) attacks |= ROCC(t) & RQU();
	  }
	  attacks &= BOARD();

	  while (attacks != 0) {
	      if ((temp = pieceb[PAWN] & colorb[c] & attacks) != 0) piece = PAWN;
	      else if ((temp = pieceb[KNIGHT] & colorb[c] & attacks) != 0) piece = KNIGHT;
	      else if ((temp = pieceb[BISHOP] & colorb[c] & attacks) != 0) piece = BISHOP;
	      else if ((temp = pieceb[ROOK] & colorb[c] & attacks) != 0) piece = ROOK;
	      else if ((temp = pieceb[QUEEN] & colorb[c] & attacks) != 0) piece = QUEEN;
	      else if ((temp = pieceb[KING] & colorb[c] & attacks) != 0) piece = KING;
	      else break;
	 
		temp &= -(long)temp;
		colorb[c] ^= temp;
		if ((piece & 4) != 0 || piece == 1) {
		  if ((piece & 1) != 0) attacks |= BOCC(t) & BQU();
		  if ((piece & 2) != 0) attacks |= ROCC(t) & RQU();
		}
		attacks &= BOARD();

		s_list[nc] = -s_list[nc - 1] + a_piece;
		a_piece = pval[piece];
		nc++;
		c ^= 1;
	  }

	  while ((--nc) != 0)
	    if (s_list[nc] > -s_list[nc - 1])
	      s_list[nc - 1] = -s_list[nc];

	  colorb[0] = colstore0;
	  colorb[1] = colstore1;
	  return s_list[0];
	}

	/* In quiesce the moves are ordered just for the value of the captured piece */
	static int qpick(int[] ml, int mn, int s) {
		int m;
		int i, t, pi = 0, vmax = -9999;
		for (i = s; i < mn; i++) {
			m = ml[i];
			t = pval[CAP(m)];
			if (t > vmax) {
				vmax = t;
				pi = i;
			}
		}
		m = ml[pi];
		if (pi != s) ml[pi] = ml[s];
		return m;
	}

	final static int[] killer = new int[128];
	final static int[] history = new int[0x1000];
	/* In normal search some basic move ordering heuristics are used */
	static int spick(int[] ml, int mn, int s, int ply) {
		int m;
		int i, pi = 0, vmax = -9999;
		for (i = s; i < mn; i++) {
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

	/* The evaluation for Color c. It's only mobility stuff. Pinned pieces are still awarded for limiting opposite's king */
	static int evalc(int c, int[] sf) {
		int f;
		int mn = 0, katt = 0;
		int oc = c^1;
		long ocb = colorb[oc];
		long m, b, a, cb;
		long kn = kmoves[kingpos[oc]];
		long pin = pinnedPieces(kingpos[c], oc);

		b = pieceb[PAWN] & colorb[c];
		while (b != 0) {
			int ppos = 0;
			f = getLsb(b);
			b ^= BIT[f];
			ppos = pawnprg[c][f];
			m = PMOVE(f, c);
			a = POCC(f, c);
			if ((a & kn) != 0) katt += _bitcnt(a & kn) << 4;
			if ((BIT[f] & pin) != 0) {
				if ((getDir(f, kingpos[c]) & 16) == 0) m = 0;
			} else {
				ppos += _bitcnt(a & pieceb[PAWN] & colorb[c]) << 2;
			}
			if (m != 0) ppos += 8; else ppos -= 8;
			if ((pawnfile[c][f] & pieceb[PAWN] & ocb) == 0) { //Free file?
				if ((pawnfree[c][f] & pieceb[PAWN] & ocb) == 0) ppos *= 2; //Free run?
				if ((pawnhelp[c][f] & pieceb[PAWN] & colorb[c]) == 0) ppos -= 33; //Hanging backpawn?
			}

			mn += ppos;
		}

		cb = colorb[c] & (~pin);
		b = pieceb[KNIGHT] & cb;
		while (b != 0) {
			sf[0] += 1;
			f = getLsb(b);
			b ^= BIT[f];
			a = nmoves[f];
			if ((a & kn) != 0) katt += _bitcnt(a & kn) << 4;
			mn += nmobil[f];
		}

		b = pieceb[KNIGHT] & pin;
		while (b != 0) {
			sf[0] += 1;
			f = getLsb(b);
			b ^= BIT[f];
			a = nmoves[f];
			if ((a & kn) != 0) katt += _bitcnt(a & kn) << 4;
		}

		colorb[oc] ^= BIT[kingpos[oc]]; //Opposite King doesn't block mobility at all
		b = pieceb[QUEEN] & cb;
		while (b != 0) {
			sf[0] += 4;
			f = getLsb(b);
			b ^= BIT[f];
			a = RATT1(f) | RATT2(f) | BATT3(f) | BATT4(f);
			if ((a & kn) != 0) katt += _bitcnt(a & kn) << 4;
			mn += bitcnt(a);
		}

		colorb[oc] ^= RQU() & ocb; //Opposite Queen & Rook doesn't block mobility for bishop
		b = pieceb[BISHOP] & cb;
		while (b != 0) {
			sf[0] += 1;
			f = getLsb(b);
			b ^= BIT[f];
			a = BATT3(f) | BATT4(f);
			if ((a & kn) != 0) katt += _bitcnt(a & kn) << 4;
			mn += bitcnt(a) << 3;
		}

		colorb[oc] ^= pieceb[ROOK] & ocb; //Opposite Queen doesn't block mobility for rook.
		colorb[c] ^= pieceb[ROOK] & cb; //Own non-pinned Rook doesn't block mobility for rook.
		b = pieceb[ROOK] & cb;
		while (b != 0) {
			sf[0] += 2;
			f = getLsb(b);
			b ^= BIT[f];
			a = RATT1(f) | RATT2(f);
			if ((a & kn) != 0) katt += _bitcnt(a & kn) << 4;
			mn += bitcnt(a) << 2;
		}

		colorb[c] ^= pieceb[ROOK] & cb; // Back
		b = pin & (pieceb[ROOK] | pieceb[BISHOP] | pieceb[QUEEN]); 
		while (b != 0) {
			f = getLsb(b);
			b ^= BIT[f];
			int p = identPiece(f);
			if (p == BISHOP) {
				sf[0] += 1; 
				a = BATT3(f) | BATT4(f);
				if ((a & kn) != 0) katt += _bitcnt(a & kn) << 4;
			} else if (p == ROOK) {
				sf[0] += 2; 
				a = RATT1(f) | RATT2(f);
				if ((a & kn) != 0) katt += _bitcnt(a & kn) << 4;
			} else {
				sf[0] += 4;
				a = RATT1(f) | RATT2(f) | BATT3(f) | BATT4(f);
				if ((a & kn) != 0) katt += _bitcnt(a & kn) << 4;
			}
			int t = p | getDir(f, kingpos[c]);
			if ((t & 10) == 10) mn += _bitcnt(RATT1(f));
			if ((t & 18) == 18) mn += _bitcnt(RATT2(f));
			if ((t & 33) == 33) mn += _bitcnt(BATT3(f));
			if ((t & 65) == 65) mn += _bitcnt(BATT4(f));
		}

		colorb[oc] ^= pieceb[QUEEN] & ocb; //Back
		colorb[oc] ^= BIT[kingpos[oc]]; //Back
		if (sf[0] == 1 && (pieceb[PAWN] & colorb[c]) == 0) mn =- 200; //No mating material
		if (sf[0] < 7) katt = katt * sf[0] / 7; //Reduce the bonus for attacking king squares
	    if (sf[0] < 2) sf[0] = 2;
		return mn + katt;
	}

	static int eval1 = 0;
	static int eval(int c) {
		int sf0 = 0, sf1 = 0;
		int[] sfp = new int[]{sf0};
		int ev0 = evalc(0, sfp);
		sf0 = sfp[0];
		sfp[0] = sf1;
		int ev1 = evalc(1, sfp);
		sf1 = sfp[0];
		eval1++;

		if (sf1 < 6) ev0 += kmobil[kingpos[0]]*(6-sf1);
		if (sf0 < 6) ev1 += kmobil[kingpos[1]]*(6-sf0);

		return (c != 0 ? (ev1 - ev0) : (ev0 - ev1));
	}

	static long nodes;
	static long qnodes;
	static int quiesce(long ch, int c, int ply, int alpha, int beta) {
		int i, w, best = -32000;
		int cmat = c == 1 ? -mat: mat;
		if (ply == 63) return eval(c) + cmat;
		
		if (ch == 0) do {
			if (cmat - 200 >= beta) return beta;
			if (cmat + 200 <= alpha) break;
			best = eval(c) + cmat;
			if (best > alpha) {
				alpha = best;
				if (best >= beta) return beta;
			}
		} while(false);

		generate(ch, c, ply, 1, 0);
		if (ch != 0 && movenum[ply] == 0) return -32000 + ply;

		for (i = 0; i < movenum[ply]; i++) {
			int m = qpick(movelist[ply], movenum[ply], i);
			if (ch == 0 && PROM(m) == 0 && pval[PIECE(m)] > pval[CAP(m)] && swap(m) < 0) continue;

			doMove(m, c);
			qnodes++;

			w = -quiesce(attacked(kingpos[c^1], c^1), c^1, ply+1, -beta, -alpha);

			undoMove(m, c);

			if (w > best) {
				best = w;
				if (w > alpha) {
					alpha = w;
					if (w >= beta) return beta;
				}
			}
		}
		return best > -32000 ? best : eval(c) + cmat;
	}

	static int retPVMove(int c, int ply) {
        int i;
        generate(attacked(kingpos[c], c), c, 0, 1, 1);
        for (i = 0; i < movenum[0]; i++) {
                int m = movelist[0][i];
                if (m == pv[0][ply]) return m;
        }
        return 0;
	}

	static boolean inputSearch() {
        int ex;
        if (!pondering) return true;       
		irbuf.append(inString.get(inStart));
		inString.remove(inStart++);

		ex = protV2(irbuf.toString());
        if (!ponder || ex != 0 || engine != onmove) pondering = false;
        if (ex == 0) irbuf.setLength(0);
        if (ex != -1) return !pondering;
        ex = parseMove(irbuf.toString(), ONMV(pon), pon);
        if (ex == 0 || ex == -1) return true;
        irbuf.setLength(0);
        pon = 0;
        return false;
	}
	
	final static int nullvar[] = new int[] {13, 43, 149, 519, 1809, 6311, 22027};
	static int nullvariance(int delta) {
	      int r = 0;
	      if (delta >= 4) for (r = 1; r <= nullvar.length; r++) if (delta < nullvar[r - 1]) break;
	      return r;
	}

	static long HASHP(int c) { return (hashb ^ hashxor[flags | 1024 | (c << 11)]); }
	static long HASHB(int c, int d) { return ((hashb ^ hashxor[flags | 1024]) ^ hashxor[c | (d << 1) | 2048]); }
	static int search(long ch, int c, int d, int ply, int alpha, int beta, int pvnode, int isnull) {
		int i, j, n, w, asave, first, best;
		int hmove;
		long hb, hp, he;
		
		pvlength[ply] = ply;
		if (ply == 63) return eval(c) + (c != 0 ? -mat: mat);
		if ((++nodes & CNODES) == 0) {
			long consumed = getTime() - starttime;
            if (!pondering && (consumed > maxtime || (consumed > searchtime && !noabort))) sabort = true;
            if (bioskey()) sabort = inputSearch();
		}
		if (sabort) return 0;

		hp = HASHP(c);
		if (ply != 0 && isDraw(hp, 1) != 0) return 0;

		if (d == 0) return quiesce(ch, c, ply, alpha, beta);
		hstack[COUNT()] = hp;

		hb = HASHB(c, d);		
		he = hashDB[(int)(hb & HMASKB)];
		if (((he^hb) & HINVB) == 0) {
			w = (int)LOW16(he) - 32768;
			if ((he & 0x10000) != 0) {
				isnull = 0;
				if (w <= alpha) return alpha;
			} else {
				if (w >= beta) return beta;
			}
		} else {
                w = c != 0 ? -mat : mat;
        }

		if (pvnode == 0 && ch == 0 && isnull != 0 && d > 1 && bitcnt(colorb[c] & (~pieceb[PAWN]) & (~pinnedPieces(kingpos[c], c^1))) > 2) {
			int flagstore = flags;
			int R = (10 + d + nullvariance(w - beta))/4;
			if (R > d) R = d;
			flags &= 960;
			count += 0x401;
			w = -search(0L, c^1, d-R, ply+1, -beta, -alpha, 0, 0); //Null Move Search
			flags = flagstore;
			count -= 0x401;
			if (!sabort && w >= beta) {
				hashDB[(int)(hb & HMASKB)] = (hb & HINVB) | (w + 32768);
				return beta;
			}
		}

		hmove = 0;
		if (ply > 0) {
			he = hashDP[(int)(hp & HMASKP)];
			if (((he^hp) & HINVP) == 0) hmove = (int)(he & HMASKP);
	
			if (d >= 4 && hmove == 0) { // Simple version of Internal Iterative Deepening
				w = search(ch, c, d-3, ply, alpha, beta, pvnode, 0);
				he = hashDP[(int)(hp & HMASKP)];
				if (((he^hp) & HINVP) == 0) hmove = (int)(he & HMASKP);
			}
		} else {
			hmove = retPVMove(c, ply);
		}

		best = pvnode != 0 ? alpha : -32001;
		asave = alpha;
		first = 1;
		for (n = 1; n <= ((ch != 0L) ? 2 : 3); n++) {
            if (n == 1) {
                if (hmove == 0) continue;
                movenum[ply] = 1;
            } else if (n == 2) {
                generate(ch, c, ply, 1, 0);
            } else {
                generate(ch, c, ply, 0, 1);
            }
            for (i = 0; i < movenum[ply]; i++) {
                int m;
                long nch;
                int ext = 0;
                if (n == 1) {
                        m = hmove;
                } else {
                        if (n == 2) m = qpick(movelist[ply], movenum[ply], i);
                        else m = spick(movelist[ply], movenum[ply], i, ply);
                        if (m == hmove) continue;
                }
			doMove(m, c);

			nch = attacked(kingpos[c^1], c^1);
			if (nch != 0) ext++; // Check Extension
			else if (d >= 3 && n == 3 && pvnode == 0) { //LMR
                if (m == killer[ply]); //Don't reduce killers
                else if (PIECE(m) == PAWN && (pawnfree[c][TO(m)] & pieceb[PAWN] & colorb[c^1]) == 0); //Don't reduce free pawns
				else ext--;
			}

			if (first != 0 && pvnode != 0) {
				w = -search(nch, c^1, d-1+ext, ply+1, -beta, -alpha, 1, 1);
				if (ply == 0) noabort = (iter > 1 && w < value[iter-1] - 40);
			} else {
				w = -search(nch, c^1, d-1+ext, ply+1, -alpha-1, -alpha, 0, 1);
				if (w > alpha && ext < 0) w = -search(nch, c^1, d-1, ply+1, -alpha-1, -alpha, 0, 1);
				if (w > alpha && w < beta && pvnode != 0) w = -search(nch, c^1, d-1+ext, ply+1, -beta, -alpha, 1, 1);
			}
			undoMove(m, c);

			if (!sabort && w > best) {
				if (w > alpha) {
					hashDP[(int)(hp & HMASKP)] = (hp & HINVP) | m;
					alpha = w;
				}
				if (w >= beta) {
					if (CAP(m) == 0) {
						killer[ply] = m;
						history[m & 0xFFF]++;
					}
					hashDB[(int)(hb & HMASKB)] = (hb & HINVB) | (w + 32768); 
					return beta;
				}
				if (pvnode != 0 && w >= alpha) {
					pv[ply][ply] = m;
					for (j = ply +1; j < pvlength[ply +1]; j++) pv[ply][j] = pv[ply +1][j];
					pvlength[ply] = pvlength[ply +1];
					if (ply == 0 && iter > 1 && w > value[iter-1] - 20) noabort = false;
					if (w == 31999 - ply) return w;
				}
				best = w;
			}
			first = 0;
		}
		}
		if (first != 0) return (ch != 0L) ? -32000+ply : 0;
        	if (pvnode != 0) {
            		if (!sabort && asave == alpha) hashDB[(int)(hb & HMASKB)] = (hb & HINVB) | 0x10000 | (asave + 32768);
        	} else {
            		if (!sabort && best < beta) hashDB[(int)(hb & HMASKB)] = (hb & HINVB) | 0x10000 | (best + 32768);
        	}
		return alpha;
	}

	static int execMove(int m) {
		int i, c;
		doMove(m, onmove);
		onmove ^= 1; 
		c = onmove;
		if (book) for (i = 0; i < BKSIZE; i++) {
			if (bkflag[i] < 2 && (bkmove[i*32 + COUNT() - 1] != m || bkmove[i*32 + COUNT()] == 0)) {
				bkcount[bkflag[i]]--;
				bkflag[i] = 2;
			}
		}
		hstack[COUNT()] = HASHP(c);
		for (i = 0; i < 127; i++) killer[i] = killer[i+1];
		for (i = 0; i < 0x1000; i++) history[i] = 0;
		i = generate(attacked(kingpos[c], c), c, 0, 1, 1);
		if (pondering) return (movenum[0] == 0 ? 7 : 0);
		if (movenum[0] == 0) {
			if (i == 0) {
				printf("1/2-1/2 {Stalemate}\n"); return 4;
			} else {
				printf(c == 1 ? "1-0 {White mates}\n" : "0-1 {Black mates}\n"); return 5 + c;
			}
		}
		switch (isDraw(HASHP(c), 2)) {
			case 1: printf("1/2-1/2 {Draw by Repetition}\n"); return 1;
			case 2: printf("1/2-1/2 {Draw by Fifty Move Rule}\n"); return 2;
			case 3: printf("1/2-1/2 {Insufficient material}\n"); return 3;
		}
		return 0;
	}

	static boolean ISRANK(int c) { return (c >= '1' && c <= '8'); }
	static boolean ISFILE(int c) { return (c >= 'a' && c <= 'h'); }
	static boolean ismove(int m, int to, int from, int piece, int prom, int h) {
        if (TO(m) != to) return false;
        if (from < 0 && PIECE(m) != piece) return false;
        if (from >= 0 && FROM(m) != from) return false;
        if (ISFILE(h) && (FROM(m) & 7) != h - 'a') return false;
        if (ISRANK(h) && (FROM(m) & 56) != 8*(h - '1')) return false;
        if (prom != 0&& PROM(m) != prom) return false;
        return true;
	}
	
	static int parseMove(String s, int c, int p) {
		int i, to, from = -1, piece = PAWN, prom = 0;
	    char h = 0, c1, c2;
		int[] ip = new int[1];
		if (s.startsWith("O-O-O")) s = c != 0 ? "Kc8" : "Kc1";
		else if (s.startsWith("O-O")) s = c != 0 ? "Kg8" : "Kg1";
		int sp = 0;
		try {
		if (s.charAt(sp) >= 'A' && s.charAt(sp) <= 'Z') if ((piece = _getpiece(s.charAt(sp++), ip)) < 1) return -1;
		if (s.charAt(sp) == 'x') sp++;
		if (ISRANK(s.charAt(sp))) {h = s.charAt(sp++); if (s.charAt(sp) == 'x') sp++; }
		if (!ISFILE(s.charAt(sp))) return -1;
		c1 = s.charAt(sp++);
		if (s.charAt(sp) == 'x') sp++;
		if (ISFILE(s.charAt(sp))) {h = c1; c1 = s.charAt(sp++);}
		c2 = s.charAt(sp++);
		if (!ISRANK(c2)) return -1;
		if (s.length() > sp) {
			if (s.charAt(sp) == '=') prom = _getpiece(s.charAt(sp + 1), ip);
			else if (s.charAt(sp) == '+');
			else { // Algebraic Notation
				from = c1 - 'a' + 8*(c2 - '1');
				c1 = s.charAt(sp++); 
				c2 = s.charAt(sp++);
				if (!ISFILE(c1) || !ISRANK(c2)) return -1;
				if (s.length() > sp) prom = _getpiece(s.charAt(sp), ip);
			}
		}
		to = c1 - 'a' + 8*(c2 - '1');
        if (p != 0) {
            if (ismove(p, to, from, piece, prom, h)) return p;
            return 0;
        }
		generate(attacked(kingpos[c], c), c, 0, 1, 1);
        for (i = 0; i < movenum[0]; i++) if (ismove(movelist[0][i], to, from, piece, prom, h)) return movelist[0][i];
		} catch (StringIndexOutOfBoundsException e) {
		}
        return 0;
	}

	static int parseMoveNExec(String s, int c, int[] m) {
        m[0] = parseMove(s, c, 0);
        if (m[0] == -1) printf("UNKNOWN COMMAND: " + s + "\n");
        else if (m[0] == 0) errprintf("Illegal move: " + s + "\n");
        else return execMove(m[0]);
        return -1;
	}

	static void undo() {
	        int cnt = COUNT() - 1;
	        onmove ^= 1;
	        undoMove((int)(mstack[cnt] >> 42L), onmove);
	}

	static int time = 30000;
	static int mps = 0;
	static int base = 5;
	static int inc = 0;
	static int st = 0;
	static boolean post = true;

	static int calc(int sd, int tm) {
			int i, j, t1 = 0, m2go = 32;
			long ch = attacked(kingpos[onmove], onmove);
			eval1 = iter = value[0] = 0;
			sabort = false;
			qnodes = nodes = 0L;
			if (mps > 0) m2go = 1 + mps - ((COUNT()/2) % mps);

			searchtime = (tm*10L)/m2go + inc*1000L;
			maxtime = inc != 0 ? tm*3L : tm*2L;
			if (st > 0) maxtime = searchtime = st*1000L;

			starttime = getTime();
			Random rand = new Random(starttime);
			if (book) {
				if (bkcount[onmove] == 0) book = false;
				else {
					j = rand.nextInt(bkcount[onmove]);
					for (i = 0; i < BKSIZE; i++) {
						if (bkflag[i] == onmove && j == t1++) { pv[0][0] = bkmove[i*32 + COUNT()]; break; }
					}
				}
			}
			if (!book) for (iter = 1; iter <= sd; iter++) {
				noabort = false;
				value[iter] = search(ch, onmove, iter, 0, -32000, 32000, 1, 0);
				t1 = (int)(getTime() - starttime);
				if (sabort && pvlength[0] == 0 && (iter--) != 0) break;
				if (post && pvlength[0] > 0) {
					System.out.printf("%2d %5d %6d %9d  ", iter, value[iter], t1/10, (int)(nodes + qnodes));
					displaypv(); printf("\n"); 
				}
				if (!pondering && (iter >= 32000-value[iter] || sabort || t1 > searchtime/2)) break;
			}
            pondering = false;
            if (pon != 0) {
                undo();
                pon = 0;
                return engine != onmove ? 1 : 0;
            }
			printf("move "); displaym(pv[0][0]); printf("\n");

			if (post) printf("\nkibitz W: " + value[iter > sd ? sd : iter] 
					+ " Nodes: " + nodes 
					+ " QNodes: " + qnodes 
					+ " Evals: " + eval1
					+ " cs: " + t1/10
					+ " knps: "+ (nodes+qnodes)/(t1+1) + "\n"); 
			return execMove(pv[0][0]);
	}

	static int doponder(int c) {
        pon = retPVMove(c, 1);
        if (pon != 0) {
                pondering = true;
                if (execMove(pon) != 0) {
                        pondering = false;
                        undo();
                        pon = 0;
                }
        }
        return pondering ? 0 : -1;
	}

	public static Map<String, Long> moves = new HashMap<>();
	public static long perft(int c, int d, int div) {
        int i, ply = 63 - d;
        long n, cnt = 0L;
        if (div == 1) moves.clear();

        generate(attacked(kingpos[c], c), c, ply, 1, 1);
        if (d == 1) return (long)movenum[ply];
        for (i = 0; i < movenum[ply]; i++) {
                int m = movelist[ply][i];
                doMove(m, c);
                cnt += n = perft(c^1, d - 1, 0);
                if (div != 0) { moves.put(displaym(m), n); printf(" " + n + "\n"); }
                undoMove(m, c);
        }
        return cnt;
	}
	
	static int protV2(String buf) {
		if (buf.startsWith("protover")) printf("feature setboard=1 myname=\"OliThink " + VER + "\" colors=0 analyze=0 done=1\n");
		else if (buf.startsWith("xboard")); 
		else if (buf.startsWith("quit")) return -2;
		else if (buf.startsWith("new")) return -3;
		else if (buf.startsWith("remove")) return -4;
		else if (buf.startsWith("force")) engine = -1;
		else if (buf.startsWith("go")) engine = pondering ? onmove^1 : onmove;
		else if (buf.startsWith("setboard")) _parse_fen(buf.substring(9));
		else if (buf.startsWith("undo")) undo();
		else if (buf.startsWith("easy")) ponder = false;
		else if (buf.startsWith("hard")) ponder = true;
		else if (buf.startsWith("sd")) sd = Integer.parseInt(buf.substring(3));
		else if (buf.startsWith("time")) time = Integer.parseInt(buf.substring(5));
		else if (buf.startsWith("level")) {
			StringTokenizer st = new StringTokenizer(buf.substring(6), " ");
			mps = Integer.parseInt(st.nextToken());
			base = Integer.parseInt(st.nextToken());
			inc = Integer.parseInt(st.nextToken());
		}
		else if (buf.startsWith("post")) post = true;
		else if (buf.startsWith("nopost")) post = false;
		else if (buf.startsWith("result"));//result 0-1 {Black mates}
		else if (buf.startsWith("otim"));//otim <optime>
		else if (buf.startsWith("draw"));//draw offer
		else if (buf.startsWith("st")) st = Integer.parseInt(buf.substring(3));
		else if (buf.startsWith("bk"));
		else if (buf.startsWith("hint"));
		else if (buf.startsWith("computer"));
		else if (buf.startsWith("accepted"));//accepted <feature>
		else if (buf.startsWith("random"));
		else if (buf.startsWith("rating"));//ICS: rating <myrat> <oprat>
		else if (buf.startsWith("name"));//ICS: name <opname>
		else if (buf.startsWith("perft")) {int i; for (i = 1; i <= sd; i++) printf("Depth: " + i + " Nodes: " + perft(onmove, i, 0) + "\n");}
        else if (buf.startsWith("divide")) perft(onmove, sd, 1);
        else return -1;
        return 0;
}
	
	static StringBuffer sbuf = new StringBuffer();
	static HashMap<Integer, String> inString = new HashMap<Integer, String>();
	static int inStart = 0;
	static int inCounter = 0;

	static void readln() 
	{
			char c = 0;
			while (c != '\n') {
			try {
					c = (char) System.in.read();
				} catch (IOException e) {
			 		e.printStackTrace();
				}
				if (c != '\n' && c!= '\r') sbuf.append(c);
			}

			inString.put(inCounter++, sbuf.toString());
			sbuf.setLength(0);
	}
	
	static int input(int c) {
			int[] m = new int[1];
		
			String buf;
			if (irbuf.length() > 0) {
				buf = irbuf.toString();
			} else {
				while (inCounter == inStart) try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	
				buf = inString.get(inStart);
				inString.remove(inStart++);
			}
			irbuf.setLength(0);
			int ex = protV2(buf);
            if (ex == -1) return parseMoveNExec(buf, c, m);
            return ex;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int i, ex = -1;
		
		ReadThread read = new ReadThread();
		read.start();

        initialize();
		
		if (args.length > 0 && "-sd".equals(args[0])) {
			time = 99999999;
			if (args.length > 1) {
				sd = Integer.parseInt(args[1]);
				if (args.length > 2) { _parse_fen(args[2]); engine = -1; }
			}
		}
		
		for (i = 0; i < 64; i++) nmobil[i] = (bitcnt(nmoves[i])-1)*6;
		for (i = 0; i < 64; i++) kmobil[i] = (bitcnt(nmoves[i])/2)*2;

		for (;;) {
			if (engine == onmove) ex = calc(sd, time);
			else if (ex == 0 && ponder && engine != -1 && !book) ex = doponder(onmove);

			if (!ponder || book || engine == -1 || ex != 0) ex = input(onmove);
			if (ex == -2) break;
			if (ex == -3) _readbook("olibook.pgn");
			if (ex == -4) { undo(); undo(); }
		}
		read.stop = true;
		try {
			System.exit(0);
		} catch (AccessControlException e) {
		}
	}

    public static void initialize() {
        for (int i = 0; i < 0x10000; i++) LSB[i] = _slow_lsb(i);
        for (int i = 0; i < 0x10000; i++) BITC[i] = _bitcnt(i);
        for (int i = 0; i < 4096; i++) hashxor[i] = _rand_64();
        for (int i = 0; i < HSIZEB; i++) hashDB[i] = 0L;
        for (int i = 0; i < HSIZEP; i++) hashDP[i] = 0L;
        for (int i = 0; i < 64; i++) BIT[i] = 1L << i;
        for (int i = 0; i < 64; i++) pmoves[0][i] = pawnfree[0][i] = pawnfile[0][i] = pawnhelp[0][i] = 0L;
        for (int i = 0; i < 192; i++) pcaps[0][i] = 0L;
        for (int i = 0; i < 64; i++) pmoves[1][i] = pawnfree[1][i] = pawnfile[1][i] = pawnhelp[1][i] = 0L;
        for (int i = 0; i < 192; i++) pcaps[1][i] = 0L;
        for (int i = 0; i < 64; i++) bmask45[i] = _bishop45(i, 0L, 0) | BIT[i];
        for (int i = 0; i < 64; i++) bmask135[i] = _bishop135(i, 0L, 0) | BIT[i];
        for (int i = 0; i < 64; i++) crevoke[i] = 0x3FF;
        for (int i = 0; i < 64; i++) kmoves[i] = nmoves[i] = 0L;
        crevoke[7] ^= BIT[6];
        crevoke[63] ^= BIT[7];
        crevoke[0] ^= BIT[8];
        crevoke[56] ^= BIT[9];

        try {
            _init_rays(0x0000, otclass, "_rook0", "key000");
            _init_rays(0x2000, otclass, "_rook90", "key090");
            _init_rays(0x4000, otclass, "_bishop45", "key045");
            _init_rays(0x6000, otclass, "_bishop135", "key135");
        } catch (Exception e) { e.printStackTrace(); }
        _init_shorts(nmoves, _knight);
        _init_shorts(kmoves, _king);
        _init_pawns(pmoves[0], pcaps[0], pawnfree[0], pawnfile[0], pawnhelp[0], 0);
        _init_pawns(pmoves[1], pcaps[1], pawnfree[1], pawnfile[1], pawnhelp[1], 1);
        _readbook("olibook.pgn");
    }
}