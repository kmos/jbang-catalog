/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package main.out;

import java.io.PrintStream;

public final class Console {

    public static final String RESET = "\033[0m";
    public static final String BOLD = "\033[1m";
    public static final String DIM = "\033[2m";
    public static final String RED = "\033[31m";
    public static final String GREEN = "\033[32m";
    public static final String YELLOW = "\033[33m";
    public static final String BLUE = "\033[34m";
    public static final String MAGENTA = "\033[35m";
    public static final String CYAN = "\033[36m";
    public static final String WHITE = "\033[37m";
    public static final String BRIGHT_GREEN = "\033[92m";
    public static final String BRIGHT_CYAN = "\033[96m";

    private Console() {
    }

    public static PrintStream out() {
        // CHECKSTYLE:OFF
        return System.out;
        // CHECKSTYLE:ON
    }

    public static void info(String msg) {
        // CHECKSTYLE:OFF
        System.out.println(CYAN + msg + RESET);
        // CHECKSTYLE:ON
    }

    public static void info(String format, Object... args) {
        // CHECKSTYLE:OFF
        System.out.printf(CYAN + format + RESET + "%n", args);
        // CHECKSTYLE:ON
    }

    public static void success(String msg) {
        // CHECKSTYLE:OFF
        System.out.println(GREEN + "✓ " + msg + RESET);
        // CHECKSTYLE:ON
    }

    public static void success(String format, Object... args) {
        // CHECKSTYLE:OFF
        System.out.printf(GREEN + "✓ " + format + RESET + "%n", args);
        // CHECKSTYLE:ON
    }

    public static void warn(String msg) {
        // CHECKSTYLE:OFF
        System.out.println(YELLOW + "⚠ " + msg + RESET);
        // CHECKSTYLE:ON
    }

    public static void error(String msg) {
        // CHECKSTYLE:OFF
        System.out.println(RED + "✗ " + msg + RESET);
        // CHECKSTYLE:ON
    }

    public static void error(String format, Object... args) {
        // CHECKSTYLE:OFF
        System.out.printf(RED + "✗ " + format + RESET + "%n", args);
        // CHECKSTYLE:ON
    }

    public static void reply(String msg) {
        // CHECKSTYLE:OFF
        System.out.print(BRIGHT_GREEN);
        var random = new java.util.Random();
        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            System.out.print(c);
            System.out.flush();
            if (c == '\n')
                continue;
            int delay = (c == '.' || c == '!' || c == '?') ? 80 + random.nextInt(120)
                    : (c == ',' || c == ';' || c == ':') ? 40 + random.nextInt(60)
                            : 15 + random.nextInt(25);
            try {
                Thread.sleep(delay);
            }
            catch (InterruptedException e) {
                break;
            }
        }
        System.out.println(RESET);
        // CHECKSTYLE:ON
    }

    public static void label(String label, String value) {
        // CHECKSTYLE:OFF
        System.out.println(DIM + label + RESET + " " + value);
        // CHECKSTYLE:ON
    }

    public static void hint(String message, String command) {
        // CHECKSTYLE:OFF
        System.out.println();
        System.out.println(YELLOW + "  ⚠ " + message + RESET);
        System.out.println(DIM + "    → Run " + RESET + BOLD + CYAN + command + RESET + DIM + " to fix this" + RESET);
        System.out.println();
        // CHECKSTYLE:ON
    }

    public static void checkOk(String service) {
        // CHECKSTYLE:OFF
        System.out.println(GREEN + "  ✓ " + RESET + service);
        // CHECKSTYLE:ON
    }

    public static void checkFail(String service, String fix) {
        // CHECKSTYLE:OFF
        System.out.println(RED + "  ✗ " + RESET + service + RED + "  ↓" + RESET);
        System.out.println(DIM + "    → " + fix + RESET);
        // CHECKSTYLE:ON
    }

    public static void item(String format, Object... args) {
        // CHECKSTYLE:OFF
        System.out.printf(WHITE + format + RESET + "%n", args);
        // CHECKSTYLE:ON
    }

    public static void header(String msg) {
        // CHECKSTYLE:OFF
        System.out.println();
        System.out.println(BOLD + BRIGHT_CYAN + msg + RESET);
        System.out.println(DIM + "─".repeat(msg.length()) + RESET);
        // CHECKSTYLE:ON
    }

    public static Spinner spinner(String message) {
        return new Spinner(message);
    }

    public static class Spinner {
        private static final String[] FRAMES = { "⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏" };
        private final String message;
        private volatile boolean running;
        private Thread thread;

        Spinner(String message) {
            this.message = message;
        }

        public void start() {
            running = true;
            thread = new Thread(() -> {
                int i = 0;
                while (running) {
                    // CHECKSTYLE:OFF
                    System.out.print("\r" + CYAN + FRAMES[i % FRAMES.length] + " " + message + RESET);
                    System.out.flush();

                    i++;
                    try {
                        Thread.sleep(80);
                    }
                    catch (InterruptedException e) {
                        break;
                    }
                }
                System.out.print("\r" + " ".repeat(message.length() + 3) + "\r");
                System.out.flush();
            }, "spinner");
            thread.setDaemon(true);
            thread.start();
            // CHECKSTYLE:ON
        }

        public void stop() {
            running = false;
            if (thread != null) {
                try {
                    thread.join(200);
                }
                catch (InterruptedException ignored) {
                }
            }
        }
    }

    public static void cooked(long millis) {
        String time;
        if (millis < 1000) {
            time = millis + "ms";
        }
        else {
            double secs = millis / 1000.0;
            time = String.format("%.1fs", secs);
        }
        // CHECKSTYLE:OFF
        System.out.println(DIM + "  🍳 Cooked in " + time + RESET);
        System.out.println();
        // CHECKSTYLE:ON
    }

    public static void welcome() {
        // CHECKSTYLE:OFF
        System.out.println(BOLD + BRIGHT_CYAN);
        System.out.println("  ██████   █████   ██████ ");
        System.out.println("  ██   ██ ██   ██ ██      ");
        System.out.println("  ██████  ███████ ██   ███");
        System.out.println("  ██   ██ ██   ██ ██    ██");
        System.out.println("  ██   ██ ██   ██  ██████ " + RESET);
        System.out.println();
        System.out.println(DIM + "  Retrieval Augmented Generation CLI" + RESET);
        System.out.println(DIM + "  Type a question to chat, /help for commands, Ctrl-D to exit" + RESET);
        System.out.println();
        // CHECKSTYLE:ON
    }
}
