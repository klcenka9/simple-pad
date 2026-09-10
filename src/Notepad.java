import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class Notepad extends JFrame {
    private final JTextPane editor = new JTextPane();
    private final StyledDocument document = editor.getStyledDocument();
    private File file;
    private boolean changed;
    private boolean loading;
    private boolean highlighting;

    private Notepad() {
        super("Simple Pad - Novy dokument");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(900, 620);
        setMinimumSize(new java.awt.Dimension(480, 320));
        setLocationByPlatform(true);
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.getInputMap().put(KeyStroke.getKeyStroke("BACK_SPACE"), "quiet-backspace");
        editor.getActionMap().put("quiet-backspace", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                try {
                    int start = editor.getSelectionStart();
                    int end = editor.getSelectionEnd();
                    if (start != end) document.remove(start, end - start);
                    else if (start > 0) document.remove(start - 1, 1);
                } catch (BadLocationException ignored) {
                    // Nothing can be deleted before the start of the document.
                }
            }
        });
        document.addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { changed(); }
            public void removeUpdate(DocumentEvent event) { changed(); }
            public void changedUpdate(DocumentEvent event) { }
        });
        add(new JScrollPane(editor), BorderLayout.CENTER);
        setJMenuBar(menuBar());
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { close(); }
        });
    }

    private JMenuBar menuBar() {
        JMenu fileMenu = new JMenu("Soubor");
        fileMenu.add(menuItem("Novy", "control N", action(event -> newDocument())));
        fileMenu.add(menuItem("Otevrit...", "control O", action(event -> open())));
        fileMenu.add(menuItem("Ulozit", "control S", action(event -> save())));
        fileMenu.add(menuItem("Ulozit jako...", "control shift S", action(event -> saveAs())));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Konec", null, action(event -> close())));
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        return menuBar;
    }

    private JMenuItem menuItem(String label, String shortcut, Action action) {
        JMenuItem item = new JMenuItem(action);
        item.setText(label);
        if (shortcut != null) item.setAccelerator(KeyStroke.getKeyStroke(shortcut));
        return item;
    }

    private Action action(java.util.function.Consumer<java.awt.event.ActionEvent> handler) {
        return new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { handler.accept(event); }
        };
    }

    private void newDocument() {
        if (!confirmClose()) return;
        file = null;
        setText("");
    }

    private void open() {
        if (!confirmClose()) return;
        JFileChooser chooser = chooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            file = chooser.getSelectedFile();
            setText(Files.readString(file.toPath()));
        } catch (IOException error) {
            showError("Nelze otevrit soubor", error.getMessage());
        }
    }

    private boolean save() {
        if (file == null) return saveAs();
        try {
            Files.writeString(file.toPath(), editor.getText());
            changed = false;
            updateTitle();
            return true;
        } catch (IOException error) {
            showError("Nelze ulozit soubor", error.getMessage());
            return false;
        }
    }

    private boolean saveAs() {
        JFileChooser chooser = chooser();
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return false;
        file = chooser.getSelectedFile();
        return save();
    }

    private JFileChooser chooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Textove soubory", "txt"));
        chooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Batch soubory", "bat", "cmd"));
        chooser.setFileFilter(chooser.getChoosableFileFilters()[1]);
        return chooser;
    }

    private boolean confirmClose() {
        if (!changed) return true;
        int choice = JOptionPane.showConfirmDialog(this, "Chcete zmeny ulozit?", "Neulozene zmeny",
            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.NO_OPTION || (choice == JOptionPane.YES_OPTION && save());
    }

    private void close() {
        if (confirmClose()) dispose();
    }

    private void changed() {
        if (!loading && !highlighting) {
            changed = true;
            updateTitle();
            SwingUtilities.invokeLater(this::highlightBatch);
        }
    }

    private void setText(String text) {
        loading = true;
        editor.setText(text);
        loading = false;
        changed = false;
        updateTitle();
        SwingUtilities.invokeLater(this::highlightBatch);
    }

    private void updateTitle() {
        setTitle("Simple Pad - " + (changed ? "*" : "") + (file == null ? "Novy dokument" : file.getName()));
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void highlightBatch() {
        if (highlighting) return;
        highlighting = true;
        try {
            String text = document.getText(0, document.getLength());
            color(0, text.length(), new Color(32, 37, 45), false);
            colorMatches(text, "(?i)\\b(echo|set|if|else|for|in|do|goto|call|exit|pause|shift|setlocal|endlocal|start|copy|move|del|mkdir|rmdir|type|findstr)\\b", new Color(44, 114, 214), true);
            colorMatches(text, "%[^%]+%", new Color(148, 91, 190), false);
            colorMatches(text, "\"[^\"]*\"", new Color(42, 139, 89), false);
            colorMatches(text, "(?m)^\\s*:[A-Za-z0-9_.-]+", new Color(190, 117, 27), true);
            colorMatches(text, "(?im)^\\s*(rem\\b.*|::.*)$", new Color(120, 128, 140), false);
        } catch (BadLocationException ignored) {
            // The text changed before syntax coloring could be applied.
        } finally {
            highlighting = false;
        }
    }

    private void colorMatches(String text, String expression, Color color, boolean bold) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(expression).matcher(text);
        while (matcher.find()) color(matcher.start(), matcher.end() - matcher.start(), color, bold);
    }

    private void color(int start, int length, Color color, boolean bold) {
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, color);
        StyleConstants.setBold(style, bold);
        document.setCharacterAttributes(start, length, style, true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Notepad().setVisible(true));
    }
}
