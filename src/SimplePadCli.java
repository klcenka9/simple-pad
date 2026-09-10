import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SimplePadCli {
    private static final String RESET = "\u001B[0m";
    private static final String BLUE = "\u001B[94m";
    private static final String GREEN = "\u001B[92m";
    private static final String PURPLE = "\u001B[95m";
    private static final String YELLOW = "\u001B[93m";
    private static final String GRAY = "\u001B[90m";
    private final List<String> lines = new ArrayList<>();
    private Path file;
    private boolean changed;

    private SimplePadCli(Path file) throws IOException {
        this.file = file;
        if (file != null && Files.exists(file)) lines.addAll(Files.readAllLines(file));
    }

    public static void main(String[] args) throws IOException {
        Path file = args.length == 0 ? null : Path.of(args[0]);
        new SimplePadCli(file).run();
    }

    private void run() throws IOException {
        System.out.println(BLUE + "Simple Pad CLI" + RESET + "  Type /help for commands.");
        if (file != null) System.out.println("File: " + file);
        show();
        try (Scanner input = new Scanner(System.in)) {
            while (true) {
                System.out.print(BLUE + "simple-pad> " + RESET);
                if (!input.hasNextLine()) return;
                String command = input.nextLine();
                if (command.equals("/help")) help();
                else if (command.equals("/show")) show();
                else if (command.startsWith("/add ")) add(command.substring(5));
                else if (command.equals("/write")) write(input);
                else if (command.startsWith("/delete ")) delete(command.substring(8));
                else if (command.equals("/save")) save(input);
                else if (command.equals("/quit") || command.equals("/exit")) {
                    if (!changed || confirmDiscard(input)) return;
                } else if (!command.isBlank()) {
                    System.out.println("Unknown command. Type /help.");
                }
            }
        }
    }

    private void help() {
        System.out.println("/show              Show the document");
        System.out.println("/add <text>        Add one line");
        System.out.println("/write             Add lines; finish with a single .");
        System.out.println("/delete <number>   Delete a line");
        System.out.println("/save              Save the document");
        System.out.println("/quit              Exit the editor");
    }

    private void show() {
        if (lines.isEmpty()) {
            System.out.println(GRAY + "(empty document)" + RESET);
            return;
        }
        for (int index = 0; index < lines.size(); index++) {
            System.out.printf(GRAY + "%4d" + RESET + "  %s%n", index + 1, highlight(lines.get(index)));
        }
    }

    private void add(String text) {
        lines.add(text);
        changed = true;
    }

    private void write(Scanner input) {
        System.out.println(GRAY + "Enter lines. Type a single . to finish." + RESET);
        while (input.hasNextLine()) {
            String line = input.nextLine();
            if (line.equals(".")) return;
            add(line);
        }
    }

    private void delete(String value) {
        try {
            int line = Integer.parseInt(value) - 1;
            if (line < 0 || line >= lines.size()) throw new NumberFormatException();
            lines.remove(line);
            changed = true;
        } catch (NumberFormatException error) {
            System.out.println("Use a valid line number.");
        }
    }

    private void save(Scanner input) throws IOException {
        if (file == null) {
            System.out.print("Save as: ");
            if (!input.hasNextLine()) return;
            String name = input.nextLine();
            if (name.isBlank()) return;
            file = Path.of(name);
        }
        Files.write(file, lines);
        changed = false;
        System.out.println(GREEN + "Saved " + file + RESET);
    }

    private boolean confirmDiscard(Scanner input) {
        System.out.print("Discard unsaved changes? [y/N] ");
        return input.hasNextLine() && input.nextLine().equalsIgnoreCase("y");
    }

    private String highlight(String line) {
        String trimmed = line.stripLeading();
        if (trimmed.startsWith("::") || trimmed.toLowerCase().startsWith("rem ")) return GRAY + line + RESET;
        if (trimmed.startsWith(":")) return YELLOW + line + RESET;
        return line.replaceAll("(?i)\\b(echo|set|if|else|for|in|do|goto|call|exit|pause|shift|setlocal|endlocal|start|copy|move|del|mkdir|rmdir|type|findstr)\\b", BLUE + "$0" + RESET)
            .replaceAll("%[^%]+%", PURPLE + "$0" + RESET)
            .replaceAll("\"[^\"]*\"", GREEN + "$0" + RESET);
    }
}
