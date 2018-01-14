package com.fmotech.chessy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.fmotech.chessy.Board.WHITE;
import static com.fmotech.chessy.StringUtils.substringAfter;
import static com.fmotech.chessy.StringUtils.substringBefore;
import static com.fmotech.chessy.StringUtils.substringBetween;

@SuppressWarnings("unused")
public class Uci {

    public static final String STARTPOS = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private static Map<String, Method> commands;
    private static Engine engine = new Engine(Board.INIT);

    public static void main(String[] args) {
        System.out.println("Chessy 2.0");
        execute();
    }

    public static void execute() {
        commands = Arrays.stream(Uci.class.getMethods())
                .filter(e -> Modifier.isStatic(e.getModifiers()))
                .filter(e -> e.getParameterTypes().length == 1 && e.getParameterTypes()[0] == String.class)
                .collect(Collectors.toMap(e -> e.getName().toLowerCase(), Function.identity()));
        Method noOp = commands.get("noop");

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String input = scanner.nextLine();
            String command = substringBefore(input, " ").toLowerCase();
            String parameters = substringAfter(input, " ");
            invoke(noOp, command, parameters);
        }
    }

    private static void invoke(Method noOp, String command, String parameters) {
        try {
            long now = System.currentTimeMillis();
            commands.getOrDefault(command, noOp).invoke(null, parameters);
            System.out.println("processing time: " + (System.currentTimeMillis() - now));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public static void uci(String parameter) {
        send("id name Chessy 2.0");
        send("id author Francisco Modesto");
        send("uciok");
    }

    public static void ucinewgame(String parameter) {
        engine = new Engine(Board.INIT);
    }

    public static void isReady(String parameter) {
        send("readyok");
    }

    public static void position(String parameter) {
        String initial = substringBefore(parameter, "moves");
        String moves = substringAfter(parameter, "moves").trim();
        String fen = initial.startsWith("fen") ? substringAfter(initial, "fen") : STARTPOS;
        String[] list = moves.isEmpty() ? new String[0] : moves.split(" ");
        engine.setup(fen, list);
    }

    public static void go(String parameter) {
        int depth = parse(parameter, "depth", 64);
        int movesToGo = parse(parameter, "movestogo", 30);
        int moveTime = parse(parameter, "movetime", 0);
        int time = engine.sideToMove() == WHITE ? parse(parameter, "wtime", 0) : parse(parameter, "btime", 0);
        int inc = engine.sideToMove() == WHITE ? parse(parameter, "winc", 0) : parse(parameter, "binc", 0);

        if (moveTime > 0) {
            time = moveTime;
            movesToGo = 1;
        }

        if (time > 0) {
            time = Math.max(1, time / movesToGo - 50 + inc);
        }
        System.out.println("Time to use: " + time);
        send("bestmove " + engine.think(time, depth));
    }

    private static int parse(String parameter, String name, int defaultValue) {
        if (!parameter.contains(name + " "))
            return defaultValue;
        return Integer.parseInt(substringBetween(parameter, name + " ", " ").trim());
    }

    public static void quit(String parameter) {
        System.exit(0);
    }

    public static void noop(String parameter) {
    }

    private static void send(String response) {
        System.out.println(response);
    }
}
