package com.fmotech.chessy;

public class StringUtils {

    private static final String EMPTY = "";
    private static final int INDEX_NOT_FOUND = -1;

    public static String substringBefore(final String str, final String separator) {
        final int pos = str.indexOf(separator);
        if (pos == INDEX_NOT_FOUND) {
            return str;
        }
        return str.substring(0, pos);
    }

    public static String substringAfter(final String str, final String separator) {
        final int pos = str.indexOf(separator);
        if (pos == INDEX_NOT_FOUND) {
            return EMPTY;
        }
        return str.substring(pos + separator.length());
    }

    public static String substringBetween(final String str, final String open, final String close) {
        final int start = str.indexOf(open);
        if (start != INDEX_NOT_FOUND) {
            final int end = str.indexOf(close, start + open.length());
            if (end != INDEX_NOT_FOUND) {
                return str.substring(start + open.length(), end);
            } else {
                return str.substring(start + open.length());
            }
        }
        return EMPTY;
    }
}
