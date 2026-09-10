import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SimplePadCli {
    private static final String CLEAR = "\u001B[2J\u001B[H";
    private static final String RESET = "\u001B[0m";
    private static final String REVERSE = "\u001B[7m";
    private static final String BLUE = "\u001B[94m";
    private static final String GREEN = "\u001B[92m";
    private static final String PURPLE = "\u001B[95m";
    private static final String YELLOW = "\u001B[93m";
    private static final String GRAY = "\u001B[90m";
    private final List<StringBuilder> lines = new ArrayList<>();
    private final Path file;
    private final int rows;
    private final int columns;
    private int row;
    private int column;
    private int scroll;
    private boolean changed;
    private String message = "^G Help  ^O Save  ^X Exit";

    private SimplePadCli(Path file) throws IOException {
        this.file = file;
        int[] size = terminalSize();
        rows = size[0];
        columns = size[1];
        if (file != null && Files.exists(file)) {
            for (String line : Files.readAllLines(file)) lines.add(new StringBuilder(line));
        }
        if (lines.isEmpty()) lines.add(new StringBuilder());
    }

    public static void main(String[] args) throws Exception {
        if (System.console() == null) {
            System.err.println("Simple Pad CLI needs an interactive terminal.");
            return;
        }
        new SimplePadCli(args.length == 0 ? null : Path.of(args[0])).run();
    }

    private void run() throws Exception {
        rawMode(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> rawMode(false)));
        try {
            while (true) {
                draw();
                int key = System.in.read();
                if (key == 24 && exit()) return;
                if (key == 15) save();
                else if (key == 7) message = "Arrows move  Backspace deletes  Enter adds line  ^O saves  ^X exits";
                else if (key == 127 || key == 8) backspace();
                else if (key == 10 || key == 13) newline();
                else if (key == 27) escape();
                else if (key >= 32 && key < 127) {
                    lines.get(row).insert(column++, (char) key);
                    changed = true;
                }
            }
        } finally {
            rawMode(false);
            System.out.print(CLEAR + RESET);
        }
    }

    private void draw() {
        int visibleRows = Math.max(1, rows - 3);
        if (row < scroll) scroll = row;
        if (row >= scroll + visibleRows) scroll = row - visibleRows + 1;
        String name = file == null ? "New Buffer" : file.getFileName().toString();
        System.out.print(CLEAR + REVERSE + "  Simple Pad CLI  " + RESET + "  " + (changed ? "Modified  " : "") + name + "\n");
        for (int screenRow = 0; screenRow < visibleRows; screenRow++) {
            int lineNumber = scroll + screenRow;
            if (lineNumber < lines.size()) {
                String number = String.format("%4d", lineNumber + 1);
                System.out.print(GRAY + number + RESET + "  " + crop(highlight(lines.get(lineNumber).toString())) + "\n");
            } else System.out.println(GRAY + "   ~" + RESET);
        }
        System.out.print(REVERSE + pad(message, columns) + RESET);
        int cursorRow = row - scroll + 2;
        int cursorColumn = Math.min(columns, column + 7);
        System.out.printf("\u001B[%d;%dH", cursorRow, cursorColumn);
        System.out.flush();
    }

    private void escape() throws IOException {
        if (System.in.read() != '[') return;
        switch (System.in.read()) {
            case 'A' -> up();
            case 'B' -> down();
            case 'C' -> column = Math.min(column + 1, lines.get(row).length());
            case 'D' -> column = Math.max(0, column - 1);
            default -> { }
        }
    }

    private void up() {
        if (row > 0) row--;
        column = Math.min(column, lines.get(row).length());
    }

    private void down() {
        if (row < lines.size() - 1) row++;
        column = Math.min(column, lines.get(row).length());
    }

    private void backspace() {
        if (column > 0) {
            lines.get(row).deleteCharAt(--column);
            changed = true;
        } else if (row > 0) {
            int previousLength = lines.get(row - 1).length();
            lines.get(row - 1).append(lines.remove(row));
            row--;
            column = previousLength;
            changed = true;
        }
    }

    private void newline() {
        StringBuilder current = lines.get(row);
        lines.add(row + 1, new StringBuilder(current.substring(column)));
        current.delete(column, current.length());
        row++;
        column = 0;
        changed = true;
    }

    private void save() throws IOException {
        if (file == null) {
            message = "Start with: simple-pad-cli filename.bat";
            return;
        }
        List<String> content = lines.stream().map(StringBuilder::toString).toList();
        Files.write(file, content);
        changed = false;
        message = "Saved " + file;
    }

    private boolean exit() {
        if (!changed) return true;
        message = "Unsaved changes. Press ^X again to exit.";
        changed = false;
        return false;
    }

    private String highlight(String line) {
        String trimmed = line.stripLeading();
        if (trimmed.startsWith("::") || trimmed.toLowerCase().startsWith("rem ")) return GRAY + line + RESET;
        if (trimmed.startsWith(":")) return YELLOW + line + RESET;
        return line.replaceAll("(?i)\\b(echo|set|if|else|for|in|do|goto|call|exit|pause|shift|setlocal|endlocal|start|copy|move|del|mkdir|rmdir|type|findstr)\\b", BLUE + "$0" + RESET)
            .replaceAll("%[^%]+%", PURPLE + "$0" + RESET)
            .replaceAll("\"[^\"]*\"", GREEN + "$0" + RESET);
    }

    private String crop(String text) {
        return text.length() > columns - 7 ? text.substring(0, Math.max(0, columns - 10)) + "..." : text;
    }

    private String pad(String text, int width) {
        return (text + " ".repeat(Math.max(0, width))).substring(0, width);
    }

    private static int[] terminalSize() {
        try {
            Process process = new ProcessBuilder("stty", "size").redirectErrorStream(true)
                .redirectInput(ProcessBuilder.Redirect.INHERIT).start();
            try (BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String[] size = output.readLine().split(" ");
                return new int[] {Integer.parseInt(size[0]), Integer.parseInt(size[1])};
            }
        } catch (Exception ignored) {
            return new int[] {24, 80};
        }
    }

    private static void rawMode(boolean enable) {
        try {
            ProcessBuilder command = enable
                ? new ProcessBuilder("stty", "-echo", "-icanon", "min", "1", "time", "0")
                : new ProcessBuilder("stty", "sane");
            command.inheritIO().start().waitFor();
        } catch (Exception ignored) {
            // The editor only runs in a terminal where stty is available.
        }
    }
}
