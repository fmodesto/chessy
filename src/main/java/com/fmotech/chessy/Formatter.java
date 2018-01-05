package com.fmotech.chessy;

import static com.fmotech.chessy.Board.BLACK;
import static com.fmotech.chessy.Board.PAWN;
import static com.fmotech.chessy.MoveTables.RATT1;
import static com.fmotech.chessy.MoveTables.RATT2;
import static com.fmotech.chessy.Utils.BIT;
import static com.fmotech.chessy.Utils.SYMBOLS;
import static com.fmotech.chessy.Utils.TEST;

public class Formatter {

    public static int moveFromSan(Board board, String move) {
        if (move.startsWith("O-O-O")) move = board.sideToMove() == BLACK ? "Kc8" : "Kc1";
        else if (move.startsWith("O-O")) move = board.sideToMove() == BLACK ? "Kg8" : "Kg1";
        move = move.replaceAll("[x+#=]", "");
        if (!move.matches("[NBRQK]?[a-h]?[1-8]?[a-h][1-8][NBRQ]?")) throw new IllegalStateException("Invalid move: " + move);

        int start = 0;
        int end = move.length() - 1;
        int piece = Character.isUpperCase(move.charAt(start)) ? SYMBOLS.indexOf(move.charAt(start++)) : PAWN;
        int promotion = Character.isDigit(move.charAt(end)) ? 0 : SYMBOLS.charAt(end--);
        int to = (move.charAt(end--) - '1') * 8 + (move.charAt(end--) - 'a');
        long target = 0;
        for (int i = start; i <= end; i++) {
            if (move.charAt(i) >= 'a' && move.charAt(i) <= 'h') target |= BIT(move.charAt(i) - 'a') | RATT2(move.charAt(i) - 'a', 0);
            if (move.charAt(i) >= '1' && move.charAt(i) <= '8') target |= BIT(move.charAt(i) - '1') | RATT1(8 * (move.charAt(i) - '1'), 0);
        }
        target = target == 0 ? -1 : target;

        int[] moves = MoveGenerator.generate(board);
        for (int i = 1; i < moves[0]; i++) {
            if (Move.piece(moves[i]) != piece) continue;
            if (Move.to(moves[i]) != to) continue;
            if (Move.promotion(moves[i]) != promotion) continue;
            if (TEST(Move.from(moves[i]), target)) return moves[i];
        }
        throw new IllegalStateException("Move not found: " + move);
    }

    public static String moveToFen(int move) {
        return String.valueOf((char)('a' + Move.from(move) % 8))
                + String.valueOf((char)('1' + Move.from(move) / 8))
                + String.valueOf((char)('a' + Move.to(move) % 8))
                + String.valueOf((char)('1' + Move.to(move) / 8))
                + (Move.promotion(move) != 0 ? String.valueOf((char)(SYMBOLS.charAt(Move.promotion(move))+32)) : "");
    }
}
