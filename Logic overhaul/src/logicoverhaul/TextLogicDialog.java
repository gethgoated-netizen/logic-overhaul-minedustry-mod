package logicoverhaul;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.event.ChangeListener;
import arc.scene.event.ChangeListener.ChangeEvent;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextArea;
import arc.scene.ui.TextField.TextFieldStyle;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;

import mindustry.Vars;
import mindustry.logic.LAssembler;
import mindustry.logic.LStatement;
import mindustry.logic.LStatements.InvalidStatement;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class TextLogicDialog extends BaseDialog {

    private static final String GITHUB_URL =
        "https://github.com/gethgoated-netizen/";

    private SyntaxTextArea codeArea;
    private Cons<String> applyCode;
    private TextFieldStyle editorStyle;

    private Label lineNumbers;
    private Label errorMarkers;
    private Label status;
    private Label diagnosticsSummary;
    private Label headerTitle;
    private Label filename;

    private Table header;
    private Table editorRows;
    private Table autocomplete;
    private Table variableList;
    private Table terminal;
    private Table diagnosticRows;

    private ScrollPane editorScroll;
    private Image githubMark;

    private final Set<Integer> errorLines =
        new LinkedHashSet<>();

    private final ArrayList<Diagnostic> diagnostics =
        new ArrayList<>();

    private ScriptLibraryDialog scriptLibrary;

    // Theme colors
    private final Color foreground = new Color();
    private final Color secondary = new Color();
    private final Color commandColor = new Color();
    private final Color variableColor = new Color();
    private final Color numberColor = new Color();
    private final Color stringColor = new Color();
    private final Color commentColor = new Color();
    private final Color labelColor = new Color();
    private final Color errorColor = new Color();
    private final Color warningColor = new Color();

    // Mindustry logic commands
    private static final String[] LOGIC_COMMANDS = {
        "set", "op", "jump", "sensor", "control",
        "radar", "uradar", "ucontrol", "ulocate",
        "getlink", "read", "write", "print",
        "printflush", "draw", "drawflush", "lookup",
        "fetch", "wait", "end", "stop", "ubind",
        "setrate", "packcolor", "unpackcolor"
    };

    // Built-in variables
    private static final String[] LOGIC_VARIABLES = {
        "@unit", "@cursorX", "@cursorY", "@mouseLeft", 
        "@mouseRight", "@this", "@health", "@maxHealth",
        "@x", "@y", "@dead", "@time", "@tick",
        "@second", "@minute", "@wave", "@waveTime",
        "@mapw", "@maph", "@links", "@counter",
        "@team", "@copper", "@lead", "@graph",
        "@power", "@enabled", "@rotation", "@shooting",
        "@ammo", "@type", "@name", "@flag",
        "@totalItems", "@firstItem", "@itemCapacity",
        "@powerNetStored", "@powerNetCapacity"
    };

    // Logic operations
    private static final String[] LOGIC_OPERATIONS = {
        "add", "sub", "mul", "div", "idiv", "mod",
        "pow", "equal", "notEqual", "land",
        "lessThan", "lessThanEq", "greaterThan",
        "greaterThanEq", "strictEqual", "shl", "shr",
        "or", "and", "xor", "max", "min",
        "angle", "angleDiff", "len", "noise", "abs",
        "log", "logn", "floor", "ceil", "sqrt",
        "rand", "sin", "cos", "tan", "asin",
        "acos", "atan"
    };

    public TextLogicDialog() {
        super("Logic Overhaul - Text Editor");

        background(Styles.black);

        cont.clear();
        cont.background(Styles.black);
        cont.defaults().pad(4f);

        // HEADER
        header = new Table(Styles.grayPanelDark);

        headerTitle = new Label(
            "LOGIC OVERHAUL",
            Styles.monoLabel
        );

        filename = new Label(
            "processor-1.mlog",
            Styles.monoLabel
        );

        header.add(headerTitle).left().pad(8f);
        header.add().growX();
        header.add(filename).right().pad(8f);

        cont.add(header).growX().row();

        // EDITOR STYLE
        editorStyle = new TextFieldStyle(Styles.areaField);

        editorStyle.font = Fonts.monospace;

        // CODE AREA
        codeArea = new SyntaxTextArea("", editorStyle);

        codeArea.setPrefRows(18);
        codeArea.setProgrammaticChangeEvents(true);

        // LINE NUMBERS
        lineNumbers = new Label(
            "1",
            Styles.monoLabel
        );

        lineNumbers.setAlignment(
            Align.top | Align.right
        );

        // ERROR MARKERS
        errorMarkers = new Label(
            "",
            Styles.monoLabel
        );

        errorMarkers.setAlignment(
            Align.top | Align.center
        );

        // EDITOR LAYOUT
        editorRows = new Table(Styles.black);
        editorRows.top().left();

        editorRows.add(errorMarkers)
            .width(18f)
            .growY()
            .top()
            .pad(6f);

        editorRows.add(lineNumbers)
            .width(36f)
            .growY()
            .top()
            .pad(6f);

        editorRows.add(codeArea)
            .grow()
            .minSize(450f, 300f);

        // SCROLLING
        editorScroll = new ScrollPane(
            editorRows,
            Styles.noBarPane
        );

        editorScroll.setFadeScrollBars(false);
        editorScroll.setScrollingDisabled(true, false);

        cont.add(editorScroll)
            .grow()
            .minSize(500f, 300f)
            .row();

        // AUTOCOMPLETE
        autocomplete = new Table(Styles.grayPanelDark);
        autocomplete.left();

        cont.add(autocomplete)
            .growX()
            .minHeight(38f)
            .maxHeight(48f)
            .row();

        // LIVE VARIABLE LIST
        variableList = new Table(Styles.grayPanelDark);
        variableList.left();

        cont.add(variableList)
            .growX()
            .minHeight(34f)
            .maxHeight(76f)
            .row();

        // DIAGNOSTICS
        terminal = new Table(Styles.grayPanelDark);

        Label terminalTitle = new Label(
            "DIAGNOSTICS",
            Styles.monoLabel
        );

        diagnosticsSummary = new Label(
            "Editor ready.",
            Styles.monoLabel
        );

        diagnosticsSummary.setWrap(true);

        diagnosticRows = new Table();
        diagnosticRows.left();

        status = new Label(
            "READY",
            Styles.monoLabel
        );

        terminal.add(terminalTitle)
            .left()
            .pad(6f)
            .row();

        terminal.add(diagnosticsSummary)
            .left()
            .growX()
            .pad(6f)
            .row();

        terminal.add(diagnosticRows)
            .left()
            .growX()
            .pad(4f)
            .row();

        terminal.add(status)
            .left()
            .pad(6f);

        cont.add(terminal)
            .growX()
            .maxHeight(150f)
            .row();

        // GITHUB CREDIT
        Table credits = new Table(Styles.grayPanelDark);

        credits.add().growX();

        TextureRegion logoRegion =
            Core.atlas.find("github", Core.atlas.white());

        githubMark = new Image(logoRegion);

        credits.add(githubMark)
            .size(20f)
            .pad(4f);

        credits.button(
            GITHUB_URL,
            () -> Core.app.openURI(GITHUB_URL)
        )
        .right()
        .pad(4f);

        cont.add(credits)
            .growX()
            .row();

        // SCRIPT LIBRARY CONNECTION
        scriptLibrary = new ScriptLibraryDialog(
            source -> {
                codeArea.setText(source);
                refreshEditor();
            },
            this::insertAtCursor,
            () -> codeArea.getText()
        );

        // LIVE EDITOR UPDATES
        codeArea.addListener(new ChangeListener() {
            @Override
            public void changed(
                ChangeEvent event,
                Element element
            ) {
                refreshEditor();
            }
        });

        // BUTTONS
        buttons.defaults().size(160f, 52f);

        buttons.button(
            "Apply to Visual Editor",
            () -> {
                try {
                    if (applyCode == null) {
                        throw new IllegalStateException(
                            "No visual editor connection."
                        );
                    }

                    applyCode.get(codeArea.getText());
                    hide();

                } catch (Throwable error) {
                    Vars.ui.showException(error);
                }
            }
        );

        buttons.button(
            "Save to Library",
            () -> scriptLibrary.showSaveDialog(
                codeArea.getText()
            )
        );

        buttons.button(
            "Script Library",
            () -> scriptLibrary.show()
        );

        buttons.button("Cancel", this::hide);

        applyTheme();
        refreshEditor();
    }

    // ==================================================
    // THEME APPLICATION
    // ==================================================

    private void applyTheme() {

        String theme = Core.settings.getString(
            LogicOverhaulMod.THEME_SETTING,
            "classic"
        );

        Palette p = paletteFor(theme);

        foreground.set(Color.valueOf(p.foreground));
        secondary.set(Color.valueOf(p.secondary));
        commandColor.set(Color.valueOf(p.command));
        variableColor.set(Color.valueOf(p.variable));
        numberColor.set(Color.valueOf(p.number));
        stringColor.set(Color.valueOf(p.string));
        commentColor.set(Color.valueOf(p.comment));
        labelColor.set(Color.valueOf(p.label));
        errorColor.set(Color.valueOf(p.error));
        warningColor.set(Color.valueOf(p.warning));

        Drawable bg = solid(p.background);
        Drawable panel = solid(p.panel);
        Drawable editor = solid(p.editor);

        background(bg);
        cont.background(bg);

        header.background(panel);
        terminal.background(panel);
        autocomplete.background(panel);
        variableList.background(panel);
        editorRows.background(editor);

        editorStyle.font = Fonts.monospace;
        editorStyle.fontColor = foreground.cpy();
        editorStyle.focusedFontColor = foreground.cpy();
        editorStyle.background = editor;
        editorStyle.focusedBackground = editor;

        codeArea.setStyle(editorStyle);

        headerTitle.setColor(commandColor);
        filename.setColor(secondary);

        lineNumbers.setColor(secondary);
        errorMarkers.setColor(errorColor);

        diagnosticsSummary.setColor(foreground);
        status.setColor(commandColor);

        githubMark.setColor(foreground);

        invalidateHierarchy();
        refreshEditor();
    }

    private Palette paletteFor(String theme) {

        switch (theme) {

            case "midnight":
                return new Palette(
                    "08111B", "111C29", "070E17",
                    "D8E2EA", "778B9D",
                    "76B7A5", "83C8B8", "E4BD70",
                    "B5D6A2", "708A7E", "C6D3B8",
                    "E56868", "D99A55"
                );

            case "light":
                return new Palette(
                    "E8ECEF", "D4DCE1", "FFFFFF",
                    "263238", "76838B",
                    "2E765A", "197D73", "A66A00",
                    "548A45", "8B969D", "76569A",
                    "CF3030", "B36A22"
                );

            case "contrast":
                return new Palette(
                    "000000", "090909", "000000",
                    "FFFFFF", "C7C7C7",
                    "FFFF00", "00FFFF", "FFB000",
                    "A8FF00", "B0B0B0", "FFFFFF",
                    "FF3333", "FF9900"
                );

            case "base":
                return new Palette(
                    "282828", "3B3B3B", "202020",
                    "E7E7E7", "A4A4A4",
                    "FFFFFF", "B5D7B8", "E5C16B",
                    "B7D5A8", "929292", "E0C38D",
                    "F06464", "E8A34F"
                );

            case "terminal":
                // Dark Windows Terminal-inspired palette.
                // No blue or purple syntax accents.
                return new Palette(
                    "101113", "191B1E", "0B0C0E",
                    "E0E2E4", "858A8F",
                    "C8B58E", "A8B99D", "D8A85D",
                    "D1C9A9", "737A70", "E0CA91",
                    "E05B5B", "D28B45"
                );

            case "classic":
            default:
                // Original Classic Terminal theme.
                return new Palette(
                    "000000", "151A16", "000000",
                    "39FF65", "536B56",
                    "70D7FF", "76E5E5", "FFD479",
                    "A6E3A1", "66836B", "C9A0FF",
                    "FF5555", "FFAA44"
                );
        }
    }

    private Drawable solid(String color) {
        return new TextureRegionDrawable(
            Core.atlas.white()
        ).tint(Color.valueOf(color));
    }

    private static class Palette {

        final String background;
        final String panel;
        final String editor;
        final String foreground;
        final String secondary;

        final String command;
        final String variable;
        final String number;
        final String string;
        final String comment;
        final String label;
        final String error;
        final String warning;

        Palette(
            String background,
            String panel,
            String editor,
            String foreground,
            String secondary,
            String command,
            String variable,
            String number,
            String string,
            String comment,
            String label,
            String error,
            String warning
        ) {
            this.background = background;
            this.panel = panel;
            this.editor = editor;
            this.foreground = foreground;
            this.secondary = secondary;

            this.command = command;
            this.variable = variable;
            this.number = number;
            this.string = string;
            this.comment = comment;
            this.label = label;
            this.error = error;
            this.warning = warning;
        }
    }

    // ==================================================
    // LIVE EDITOR REFRESH
    // ==================================================

    private void refreshEditor() {

        if (codeArea == null) return;

        String source = codeArea.getText();

        validateCode(source);

        String[] lines = source.split("\n", -1);

        int count = Math.max(1, lines.length);

        StringBuilder numbers = new StringBuilder();
        StringBuilder markers = new StringBuilder();

        for (int i = 1; i <= count; i++) {

            numbers.append(i);

            markers.append(
                errorLines.contains(i) ? "!" : " "
            );

            if (i < count) {
                numbers.append('\n');
                markers.append('\n');
            }
        }

        lineNumbers.setText(numbers.toString());
        errorMarkers.setText(markers.toString());

        codeArea.setPrefRows(Math.max(18, count));

        editorRows.invalidateHierarchy();
        editorScroll.invalidateHierarchy();

        updateAutocomplete();
        updateVariableList(source);
    }

    // ==================================================
    // AUTOCOMPLETE
    // ==================================================

    private void updateAutocomplete() {

        autocomplete.clear();

        String source = codeArea.getText();

        int cursor = codeArea.getCursorPosition();

        if (cursor < 0 || cursor > source.length()) return;

        int tokenStart = cursor;

        while (tokenStart > 0) {

            char c = source.charAt(tokenStart - 1);

            if (
                Character.isWhitespace(c) ||
                c == ';' ||
                c == ','
            ) {
                break;
            }

            tokenStart--;
        }

        String prefix = source.substring(tokenStart, cursor);

        if (prefix.isEmpty()) {
            addHint(
                autocomplete,
                "SUGGESTIONS: TYPE A COMMAND OR VARIABLE"
            );
            return;
        }

        int lineStart = source.lastIndexOf(
            '\n',
            Math.max(0, tokenStart - 1)
        ) + 1;

        String beforeToken = source.substring(
            lineStart,
            tokenStart
        );

        int semicolon = beforeToken.lastIndexOf(';');

        if (semicolon >= 0) {
            beforeToken = beforeToken.substring(semicolon + 1);
        }

        boolean commandPosition =
            beforeToken.trim().isEmpty();

        ArrayList<String> matches = new ArrayList<>();

        if (commandPosition) {

            addMatching(
                matches,
                LOGIC_COMMANDS,
                prefix
            );

        } else {

            addMatching(
                matches,
                LOGIC_VARIABLES,
                prefix
            );

            addMatching(
                matches,
                LOGIC_OPERATIONS,
                prefix
            );

            addMatching(
                matches,
                new String[]{
                    "true",
                    "false",
                    "null",
                    "always"
                },
                prefix
            );

            addMatching(
                matches,
                declaredVariables(source).toArray(
                    new String[0]
                ),
                prefix
            );
        }

        if (matches.isEmpty()) {
            addHint(autocomplete, "NO MATCHING SUGGESTIONS");
            return;
        }

        int shown = Math.min(6, matches.size());

        for (int i = 0; i < shown; i++) {

            String suggestion = matches.get(i);

            final int start = tokenStart;
            final int end = cursor;

            autocomplete.button(
                suggestion,
                () -> insertSuggestion(
                    suggestion,
                    start,
                    end
                )
            )
            .size(110f, 32f)
            .pad(2f);
        }
    }

    private void addHint(Table table, String message) {

        Label hint = new Label(
            message,
            Styles.monoLabel
        );

        hint.setColor(secondary);

        table.add(hint)
            .left()
            .pad(5f);
    }

    private void addMatching(
        ArrayList<String> output,
        String[] choices,
        String prefix
    ) {

        String lower = prefix.toLowerCase(Locale.ROOT);

        for (String choice : choices) {

            if (
                choice.toLowerCase(Locale.ROOT).startsWith(lower) &&
                !output.contains(choice)
            ) {
                output.add(choice);
            }
        }
    }

    private void insertSuggestion(
        String suggestion,
        int start,
        int end
    ) {

        String source = codeArea.getText();

        if (
            start < 0 ||
            end > source.length() ||
            start > end
        ) {
            return;
        }

        String updated =
            source.substring(0, start)
            + suggestion
            + source.substring(end);

        codeArea.setText(updated);

        codeArea.setCursorPosition(
            start + suggestion.length()
        );

        refreshEditor();
    }

    // ==================================================
    // INSERT AT CURSOR
    // ==================================================

    private void insertAtCursor(String insertion) {

        if (insertion == null || insertion.isEmpty()) {
            return;
        }

        String source = codeArea.getText();

        int cursor = Math.max(
            0,
            Math.min(
                codeArea.getCursorPosition(),
                source.length()
            )
        );

        String updated =
            source.substring(0, cursor)
            + insertion
            + source.substring(cursor);

        codeArea.setText(updated);

        codeArea.setCursorPosition(
            cursor + insertion.length()
        );

        refreshEditor();
    }

    // ==================================================
    // LIVE VARIABLE LIST
    // ==================================================

    private void updateVariableList(String source) {

        variableList.clear();

        variableList.add("VARIABLES")
            .left()
            .pad(5f);

        Set<String> names = declaredVariables(source);

        if (names.isEmpty()) {

            Label empty = new Label(
                "No user variables declared yet",
                Styles.monoLabel
            );

            empty.setColor(secondary);

            variableList.add(empty)
                .left()
                .pad(5f);

            return;
        }

        for (String name : names) {

            variableList.button(
                name,
                () -> insertAtCursor(name)
            )
            .size(100f, 28f)
            .pad(2f);
        }
    }

    private Set<String> declaredVariables(String source) {

        Set<String> names = new LinkedHashSet<>();

        String[] lines = source.split("\n");

        for (String line : lines) {

            String trimmed = line.trim();

            if (
                trimmed.isEmpty() ||
                trimmed.startsWith("#")
            ) {
                continue;
            }

            String[] parts = trimmed.split("\\s+");

            if (parts.length < 2) continue;

            String command = parts[0];

            if (
                command.equals("set") ||
                command.equals("op") ||
                command.equals("sensor") ||
                command.equals("read") ||
                command.equals("getlink") ||
                command.equals("lookup") ||
                command.equals("fetch") ||
                command.equals("getflag")
            ) {

                String name = parts[1];

                if (
                    !name.startsWith("@") &&
                    !name.endsWith(":")
                ) {
                    names.add(name);
                }
            }
        }

        return names;
    }

    // ==================================================
    // MINDUSTRY ASSEMBLER VALIDATION
    // ==================================================

    private void validateCode(String source) {

        errorLines.clear();
        diagnostics.clear();

        String[] lines = source.split("\n", -1);

        for (int i = 0; i < lines.length; i++) {

            String trimmed = lines[i].trim();

            if (
                trimmed.isEmpty() ||
                trimmed.startsWith("#") ||
                trimmed.endsWith(":")
            ) {
                continue;
            }

            if (isNamedJump(trimmed)) continue;

            try {

                Seq<LStatement> parsed =
                    LAssembler.read(lines[i], false);

                for (LStatement statement : parsed) {

                    if (statement instanceof InvalidStatement) {

                        addDiagnostic(
                            i + 1,
                            "Invalid instruction or arguments."
                        );

                        break;
                    }
                }

            } catch (Throwable error) {

                addDiagnostic(
                    i + 1,
                    errorMessage(error)
                );
            }
        }

        // Check the complete script as well.
        try {

            LAssembler.assemble(source, false);

        } catch (Throwable error) {

            String message = errorMessage(error);

            boolean alreadyShown = false;

            for (Diagnostic diagnostic : diagnostics) {

                if (diagnostic.message.equals(message)) {
                    alreadyShown = true;
                    break;
                }
            }

            if (!alreadyShown) {
                diagnostics.add(
                    new Diagnostic(0, "Script: " + message)
                );
            }
        }

        diagnosticRows.clear();

        if (diagnostics.isEmpty()) {

            diagnosticsSummary.setText(
                "No instruction errors detected."
            );

            Label none = new Label(
                "No errors found by Mindustry's logic assembler.",
                Styles.monoLabel
            );

            none.setColor(commandColor);

            diagnosticRows.add(none).left();

            status.setText(
                "READY | " + lines.length +
                " LINES | " +
                declaredVariables(source).size() +
                " VARIABLES"
            );

            status.setColor(commandColor);

        } else {

            diagnosticsSummary.setText(
                diagnostics.size() +
                " issue(s) found. Select an entry to jump to its line."
            );

            for (Diagnostic diagnostic : diagnostics) {

                final int line = diagnostic.line;

                String text =
                    (line > 0 ? "LINE " + line + ": " : "SCRIPT: ")
                    + diagnostic.message;

                diagnosticRows.button(
                    text,
                    () -> {
                        if (line > 0) {
                            jumpToLine(line);
                        }
                    }
                )
                .growX()
                .left()
                .pad(2f)
                .row();
            }

            status.setText(
                "ERRORS: " + diagnostics.size()
            );

            status.setColor(errorColor);
        }
    }

    private void addDiagnostic(
        int line,
        String message
    ) {

        if (line > 0) {
            errorLines.add(line);
        }

        for (Diagnostic existing : diagnostics) {

            if (
                existing.line == line &&
                existing.message.equals(message)
            ) {
                return;
            }
        }

        diagnostics.add(
            new Diagnostic(line, message)
        );
    }

    private void jumpToLine(int line) {

        String source = codeArea.getText();

        int offset = 0;

        String[] lines = source.split("\n", -1);

        for (
            int i = 1;
            i < line && i <= lines.length;
            i++
        ) {
            offset += lines[i - 1].length() + 1;
        }

        codeArea.setCursorPosition(
            Math.min(offset, source.length())
        );

        if (Core.scene != null) {
            Core.scene.setKeyboardFocus(codeArea);
        }
    }

    private boolean isNamedJump(String line) {

        String[] parts = line.split("\\s+");

        if (
            parts.length < 2 ||
            !parts[0].equals("jump")
        ) {
            return false;
        }

        try {

            Integer.parseInt(parts[1]);
            return false;

        } catch (NumberFormatException ignored) {
            return true;
        }
    }

    private String errorMessage(Throwable error) {

        String message = error.getMessage();

        if (
            message == null ||
            message.trim().isEmpty()
        ) {
            return error.getClass().getSimpleName();
        }

        return message.replace('\n', ' ')
            .replace('\r', ' ');
    }

    private static class Diagnostic {

        final int line;
        final String message;

        Diagnostic(int line, String message) {
            this.line = line;
            this.message = message;
        }
    }

    // ==================================================
    // SYNTAX HIGHLIGHTING
    // ==================================================

    private class SyntaxTextArea extends TextArea {

        SyntaxTextArea(
            String text,
            TextFieldStyle style
        ) {
            super(text, style);
        }

        @Override
        protected void drawText(
            Font font,
            float x,
            float y
        ) {

            Color oldColor = font.getColor().cpy();

            float offsetY = 0f;

            for (
                int i = firstLineShowing * 2;
                i < (firstLineShowing + linesShowing) * 2 &&
                    i < linesBreak.size;
                i += 2
            ) {

                int start = linesBreak.items[i];
                int end = linesBreak.items[i + 1];

                int physicalLine = getPhysicalLine(start);

                drawHighlightedLine(
                    font,
                    x,
                    y + offsetY,
                    start,
                    end,
                    physicalLine
                );

                // Red underline for detected errors.
                if (errorLines.contains(physicalLine)) {

                    float width =
                        glyphPositions.get(end) -
                        glyphPositions.get(start);

                    Draw.color(errorColor);
                    Lines.stroke(1.2f);

                    Lines.line(
                        x,
                        y + offsetY - 3f,
                        x + Math.max(width, 8f),
                        y + offsetY - 3f
                    );

                    Draw.reset();
                }

                offsetY -= font.getLineHeight();
            }

            font.setColor(oldColor);
        }

        private void drawHighlightedLine(
            Font font,
            float x,
            float y,
            int start,
            int end,
            int physicalLine
        ) {

            int pos = start;

            boolean firstToken = true;

            while (pos < end) {

                char c = text.charAt(pos);

                if (Character.isWhitespace(c)) {
                    pos++;
                    continue;
                }

                // COMMENTS
                if (c == '#') {

                    drawRange(
                        font,
                        x,
                        y,
                        start,
                        pos,
                        end,
                        commentColor
                    );

                    break;
                }

                // STRINGS
                if (c == '"') {

                    int tokenEnd = pos + 1;

                    while (tokenEnd < end) {

                        char next = text.charAt(tokenEnd);

                        if (
                            next == '\\' &&
                            tokenEnd + 1 < end
                        ) {
                            tokenEnd += 2;
                            continue;
                        }

                        tokenEnd++;

                        if (next == '"') {
                            break;
                        }
                    }

                    drawRange(
                        font,
                        x,
                        y,
                        start,
                        pos,
                        tokenEnd,
                        stringColor
                    );

                    pos = tokenEnd;
                    firstToken = false;

                    continue;
                }

                // SEMICOLON
                if (c == ';') {

                    drawRange(
                        font,
                        x,
                        y,
                        start,
                        pos,
                        pos + 1,
                        secondary
                    );

                    pos++;
                    firstToken = true;

                    continue;
                }

                int tokenEnd = pos;

                while (tokenEnd < end) {

                    char next = text.charAt(tokenEnd);

                    if (
                        Character.isWhitespace(next) ||
                        next == ';' ||
                        next == '#'
                    ) {
                        break;
                    }

                    tokenEnd++;
                }

                String token = text.substring(pos, tokenEnd);

                Color tokenColor;

                if (
                    firstToken &&
                    errorLines.contains(physicalLine)
                ) {
                    tokenColor = errorColor;

                } else if (firstToken) {

                    tokenColor = commandColor;

                } else if (token.endsWith(":")) {

                    tokenColor = labelColor;

                } else {

                    tokenColor = classifyToken(token);
                }

                drawRange(
                    font,
                    x,
                    y,
                    start,
                    pos,
                    tokenEnd,
                    tokenColor
                );

                pos = tokenEnd;
                firstToken = false;
            }
        }

        private Color classifyToken(String token) {

            if (
                token.startsWith("@") ||
                token.startsWith("_")
            ) {
                return variableColor;
            }

            if (
                token.equals("true") ||
                token.equals("false") ||
                token.equals("null")
            ) {
                return labelColor;
            }

            if (isNumber(token)) {
                return numberColor;
            }

            return foreground;
        }

        private boolean isNumber(String token) {

            try {

                Double.parseDouble(token);
                return true;

            } catch (NumberFormatException ignored) {
                return token.matches(
                    "(?i)[+-]?0x[0-9a-f]+" +
                    "|[+-]?0b[01]+" +
                    "|%[0-9a-f]{6}([0-9a-f]{2})?"
                );
            }
        }

        private void drawRange(
            Font font,
            float x,
            float y,
            int lineStart,
            int start,
            int end,
            Color color
        ) {

            if (end <= start) return;

            float offset =
                glyphPositions.get(start) -
                glyphPositions.get(lineStart);

            font.setColor(color);

            font.draw(
                displayText,
                x + offset,
                y,
                start,
                end,
                0f,
                Align.left,
                false
            );
        }

        private int getPhysicalLine(int position) {

            int line = 1;

            for (int i = 0; i < position; i++) {

                if (text.charAt(i) == '\n') {
                    line++;
                }
            }

            return line;
        }
    }

    // ==================================================
    // OPEN EDITOR
    // ==================================================

    public void showEditor(
        String currentCode,
        Cons<String> apply
    ) {

        applyCode = apply;

        codeArea.setText(
            currentCode == null ? "" : currentCode
        );

        applyTheme();
        refreshEditor();

        show();
    }
}
