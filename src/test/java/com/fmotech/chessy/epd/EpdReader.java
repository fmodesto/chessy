package com.fmotech.chessy.epd;

import com.fmotech.chessy.Board;
import com.fmotech.chessy.Formatter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.ordinalIndexOf;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

public class EpdReader {

    private static final PathMatcher EPD = FileSystems.getDefault().getPathMatcher("glob:**.epd");

    public static List<String> getMoves(Epd epd, String type) {
        return epd.actions.stream()
                .filter(e -> type.equals(e.action))
                .flatMap(e -> Arrays.stream(StringUtils.split(e.parameter, " ")))
                .map(e -> Formatter.moveToFen(Formatter.moveFromSan(epd.board, e)))
                .collect(toList());
    }

    public static String getFen(Epd epd, String type) {
        return epd.actions.stream()
                .filter(e -> type.equals(e.action))
                .flatMap(e -> Arrays.stream(StringUtils.split(e.parameter, " ")))
                .map(e -> Formatter.moveToFen(Formatter.moveFromSan(epd.board, e)))
                .collect(joining(","));
    }

    public static class Action {
        public final String action;
        public final String parameter;

        public Action(String action, String parameter) {
            this.action = action;
            this.parameter = parameter;
        }

        @Override
        public String toString() {
            return "Action{action='" + action + '\'' + ", parameter='" + parameter + '\'' + '}';
        }
    }

    public static class Epd {
        public final String fen;
        public final Board board;
        public final List<Action> actions;

        public Epd(String fen, List<Action> actions) {
            this.fen = fen;
            this.board = Board.load(fen);
            this.actions = actions;
        }

        @Override
        public String toString() {
            return "Epd{board=" + board + ", actions=" + actions + '}';
        }
    }

    public static Stream<Epd> read(Path path) {
        return find(path).flatMap(EpdReader::parse);
    }

    private static Stream<Path> find(Path path) {
        try {
            return Files.isDirectory(path) ? Files.list(path).flatMap(EpdReader::find) : Stream.of(path).filter(EPD::matches);
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private static Stream<Epd> parse(Path path) {
        try {
            return Files.lines(path, StandardCharsets.ISO_8859_1)
                    .map(StringUtils::normalizeSpace)
                    .map(EpdReader::parseLine)
                    .filter(Objects::nonNull);
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private static Epd parseLine(final String line) {
        try {
            String fen = StringUtils.normalizeSpace(substring(line, 0, ordinalIndexOf(line, " ", 4)) + " 0 1");
            String operations = substring(line, ordinalIndexOf(line, " ", 4) + 1);
            List<Action> actions = Arrays.stream(split(operations, ";"))
                    .filter(StringUtils::isNotBlank)
                    .map(StringUtils::normalizeSpace)
                    .map(e -> new Action(substringBefore(e, " "), substringAfter(e, " ")))
                    .filter(e -> Arrays.asList("D1", "D2", "D3", "D4", "D5", "D6", "am", "bm", "pm").contains(e.action))
                    .sorted(Comparator.comparing(e -> e.action))
                    .collect(toList());
            return actions.isEmpty() ? null : new Epd(fen, actions);
        } catch (Exception e) {
            System.err.println("In: " + line);
            return null;
        }
    }
}
