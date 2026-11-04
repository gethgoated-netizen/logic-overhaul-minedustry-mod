package logicoverhaul;

import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Lines;
import arc.scene.Element;
import arc.scene.event.ChangeListener;
import arc.scene.event.ChangeListener.ChangeEvent;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextArea;
import arc.scene.ui.TextField.TextFieldStyle;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;

import mindustry.Vars;
import mindustry.ui.Fonts;
import mindustry.logic.LAssembler;
import mindustry.logic.LStatement;
import mindustry.logic.LStatements.InvalidStatement;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TextLogicDialog extends BaseDialog {

    private SyntaxTextArea codeArea;
    private Cons<String> applyCode;

    private Label lineNumbers;
    private Label errorMarkers;
    private Label diagnostics;
    private Label status;

    private Table editorRows;
    private ScrollPane editorScroll;

    private final Set<Integer> errorLines = new HashSet<>();

    private static final Color GREEN =
        Color.valueOf("39FF65");

    private static final Color DIM_GREEN =
        Color.valueOf("536B56");

    private static final Color COMMAND =
        Color.valueOf("70D7FF");

    private static final Color VARIABLE =
        Color.valueOf("76E5E5");

    private static final Color NUMBER =
        Color.valueOf("FFD479");

    private static final Color STRING =
        Color.valueOf("A6E3A1");

    private static final Color COMMENT =
        Color.valueOf("66836B");

    private static final Color LABEL =
        Color.valueOf("C9A0FF");

    private static final Color ERROR =
        Color.valueOf("FF5555");

    public TextLogicDialog() {
        super("Logic Overhaul - Text Editor");

        background(Styles.black);

        cont.clear();
        cont.background(Styles.black);
        cont.defaults().pad(4f);

        // HEADER
        Table header = new Table(Styles.grayPanelDark);

        Label title = new Label(
            "LOGIC OVERHAUL",
            Styles.monoLabel
        );

        title.setColor(GREEN);

        Label filename = new Label(
            "processor-1.mlog",
            Styles.monoLabel
        );

        filename.setColor(DIM_GREEN);

        header.add(title).left().pad(8f);
        header.add().growX();
        header.add(filename).right().pad(8f);

        cont.add(header).growX().row();

        // EDITOR STYLE
        TextFieldStyle editorStyle =
            new TextFieldStyle(Styles.areaField);

        editorStyle.font = Fonts.monospace;
        editorStyle.fontColor = GREEN;
        editorStyle.focusedFontColor = GREEN;

        editorStyle.background = Styles.black;
        editorStyle.focusedBackground = Styles.black;

        // CODE AREA
        codeArea = new SyntaxTextArea(
            "",
            editorStyle
        );

        codeArea.setPrefRows(18);
        codeArea.setProgrammaticChangeEvents(true);

        // LINE NUMBERS
        lineNumbers = new Label(
            "1",
            Styles.monoLabel
        );

        lineNumbers.setColor(DIM_GREEN);
        lineNumbers.setAlignment(
            Align.top | Align.right
        );

        // ERROR MARKERS
        errorMarkers = new Label(
            "",
            Styles.monoLabel
        );

        errorMarkers.setColor(ERROR);
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

        // TERMINAL
        Table terminal = new Table(
            Styles.grayPanelDark
        );

        Label terminalTitle = new Label(
            "TERMINAL / DIAGNOSTICS",
            Styles.monoLabel
        );

        terminalTitle.setColor(GREEN);

        diagnostics = new Label(
            "Editor ready.",
            Styles.monoLabel
        );

        diagnostics.setColor(GREEN);
        diagnostics.setWrap(true);

        status = new Label(
            "READY",
            Styles.monoLabel
        );

        status.setColor(GREEN);

        terminal.add(terminalTitle)
            .left()
            .pad(6f)
            .row();

        terminal.add(diagnostics)
            .left()
            .growX()
            .pad(6f)
            .row();

        terminal.add(status)
            .left()
            .pad(6f);

        cont.add(terminal)
            .growX()
            .maxHeight(110f)
            .row();

        // LIVE VALIDATION
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
        buttons.defaults().size(180f, 54f);

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

        buttons.button("Cancel", this::hide);
    }

    // UPDATE EDITOR INFORMATION
    private void refreshEditor() {

        String source = codeArea.getText();

        validateCode(source);

        String[] lines = source.split("\n", -1);

        int count = Math.max(1, lines.length);

        StringBuilder numbers = new StringBuilder();
        StringBuilder markers = new StringBuilder();

        for (int i = 1; i <= count; i++) {

            numbers.append(i);

            if (i < count) {
                numbers.append('\n');
            }

            markers.append(
                errorLines.contains(i) ? "!" : " "
            );

            if (i < count) {
                markers.append('\n');
            }
        }

        lineNumbers.setText(numbers.toString());
        errorMarkers.setText(markers.toString());

        codeArea.setPrefRows(
            Math.max(18, count)
        );

        editorRows.invalidateHierarchy();
        editorScroll.invalidateHierarchy();
    }

    // CHECK CODE USING MINDUSTRY'S LOGIC PARSER
    private void validateCode(String source) {

        errorLines.clear();

        String[] lines = source.split("\n", -1);

        ArrayList<String> messages =
            new ArrayList<>();

        int errorCount = 0;

        for (int i = 0; i < lines.length; i++) {

            String trimmed = lines[i].trim();

            if (
                trimmed.isEmpty() ||
                trimmed.startsWith("#") ||
                trimmed.endsWith(":")
            ) {
                continue;
            }

            // Named jumps need the complete script
            // because their destination label may be later.
            if (isNamedJump(trimmed)) {
                continue;
            }

            try {

                Seq<LStatement> parsed =
                    LAssembler.read(lines[i], false);

                boolean invalid = false;

                for (LStatement statement : parsed) {
                    if (statement instanceof InvalidStatement) {
                        invalid = true;
                        break;
                    }
                }

                if (invalid) {
                    errorLines.add(i + 1);
                    errorCount++;

                    messages.add(
                        "Line " + (i + 1) +
                        ": Invalid instruction or arguments."
                    );
                }

            } catch (Throwable error) {

                errorLines.add(i + 1);
                errorCount++;

                messages.add(
                    "Line " + (i + 1) +
                    ": " + errorMessage(error)
                );
            }
        }

        // Check the complete script for errors
        // such as missing jump labels.
        try {

            LAssembler.assemble(source, false);

        } catch (Throwable error) {

            String message = errorMessage(error);

            messages.add(
                "Script: " + message
            );

            errorCount++;
        }

        if (errorCount == 0) {

            diagnostics.setText(
                "No instruction errors detected."
            );

            status.setText(
                "READY | " + lines.length + " LINES"
            );

            status.setColor(GREEN);

        } else {

            StringBuilder output = new StringBuilder();

            int shown = Math.min(4, messages.size());

            for (int i = 0; i < shown; i++) {

                output.append(messages.get(i));

                if (i < shown - 1) {
                    output.append('\n');
                }
            }

            if (messages.size() > shown) {
                output.append("\n...and more errors.");
            }

            diagnostics.setText(output.toString());

            status.setText(
                "ERRORS: " + errorCount
            );

            status.setColor(ERROR);
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

        return message
            .replace('\n', ' ')
            .replace('\r', ' ');
    }

    // SYNTAX-HIGHLIGHTED TEXT AREA
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

                // Draw a red underline on invalid lines.
                if (errorLines.contains(physicalLine)) {

                    float width =
                        glyphPositions.get(end) -
                        glyphPositions.get(start);

                    Draw.color(ERROR);
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

                // Ignore spaces; glyph positions
                // preserve their original spacing.
                if (Character.isWhitespace(c)) {
                    pos++;
                    continue;
                }

                // Comments
                if (c == '#') {

                    drawRange(
                        font, x, y, start,
                        pos, end, COMMENT
                    );

                    break;
                }

                // Strings
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
                        font, x, y, start,
                        pos, tokenEnd, STRING
                    );

                    pos = tokenEnd;
                    firstToken = false;

                    continue;
                }

                // Semicolon begins another instruction.
                if (c == ';') {

                    drawRange(
                        font, x, y, start,
                        pos, pos + 1, DIM_GREEN
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

                String token =
                    text.substring(pos, tokenEnd);

                Color tokenColor;

                if (
                    firstToken &&
                    errorLines.contains(physicalLine)
                ) {
                    tokenColor = ERROR;

                } else if (firstToken) {
                    tokenColor = COMMAND;

                } else if (token.endsWith(":")) {
                    tokenColor = LABEL;

                } else {
                    tokenColor = classifyToken(token);
                }

                drawRange(
                    font, x, y, start,
                    pos, tokenEnd, tokenColor
                );

                pos = tokenEnd;
                firstToken = false;
            }
        }

        private Color classifyToken(String token) {

            if (token.startsWith("@") || token.startsWith("_")) {
                return VARIABLE;
            }

            if (
                token.equals("true") ||
                token.equals("false") ||
                token.equals("null")
            ) {
                return LABEL;
            }

            if (isNumber(token)) {
                return NUMBER;
            }

            return GREEN;
        }

        private boolean isNumber(String token) {

            try {
                Double.parseDouble(token);
                return true;
            } catch (NumberFormatException ignored) {
                // Continue checking other number formats.
            }

            return token.matches(
                "(?i)[+-]?0x[0-9a-f]+" +
                "|[+-]?0b[01]+" +
                "|%[0-9a-f]{6}([0-9a-f]{2})?"
            );
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

            if (end <= start) {
                return;
            }

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

    // OPEN EDITOR
    public void showEditor(
        String currentCode,
        Cons<String> apply
    ) {

        applyCode = apply;

        codeArea.setText(currentCode);

        refreshEditor();

        show();
    }
}
