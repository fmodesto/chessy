package com.fmotech.chessy;

public class DebugUtils {

    public static String toHexString(long l) {
        return String.format("%016x", l);
    }

    public static String toHexString(int l) {
        return String.format("%08x", l);
    }

    public static String debug(long... bitBoards) {
        StringBuilder sb = new StringBuilder();
        for (int i = 7; i >= 0; i--) {
            for (int k = 0; k < bitBoards.length; k++) {
                int rank = (int) (bitBoards[k] >>> (8 * i)) & 0xFF;
                for (int j = 1; j < 256; j *= 2) {
                    sb.append(((rank & j) != 0 ? "X " : "· "));
                }
                sb.append("   ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
