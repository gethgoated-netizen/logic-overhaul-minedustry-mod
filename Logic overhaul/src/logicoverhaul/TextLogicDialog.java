package logicoverhaul;

import arc.func.Cons;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.event.ChangeListener;
import arc.scene.event.ChangeListener.ChangeEvent;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextArea;
import arc.scene.ui.TextField.TextFieldStyle;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.graphics.Fonts;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class TextLogicDialog extends BaseDialog {

    private TextArea codeArea;
    private Cons<String> applyCode;

    private Label lineNumbers;
    private Label diagnostics;
    private Label status;

    private Table editorRows;
    private ScrollPane editorScroll;

    private static final Color GREEN =
        Color.valueOf("39FF65");

    private static final Color DIM_GREEN =
        Color.valueOf("536B56");

    private static final Color LIGHT_GREEN =
        Color.valueOf("8CFF9D");

    public TextLogicDialog() {
        super("Logic Overhaul - Text Editor");

        background(Styles.black);

        // Main dialog background
        cont.clear();
        cont.background(Styles.black);
        cont.defaults().pad(4f);

        // Header
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

        filename.setColor(LIGHT_GREEN);

        header.add(title).left().pad(8f);
        header.add().growX();
        header.add(filename).right().pad(8f);

        cont.add(header).growX().row();

        // Copy the existing Mindustry editor style
        // and customize it for our terminal.
        TextFieldStyle editorStyle =
            new TextFieldStyle(Styles.areaField);

        editorStyle.font = Fonts.monospace;
        editorStyle.fontColor = GREEN;
        editorStyle.focusedFontColor = GREEN;
        editorStyle.background = Styles.black;
        editorStyle.focusedBackground = Styles.black;

        // Code editor
        codeArea = new TextArea("", editorStyle);

        codeArea.setPrefRows(18);
        codeArea.setProgrammaticChangeEvents(true);

        // Line number display
        lineNumbers = new Label(
            "1",
            Styles.monoLabel
        );

        lineNumbers.setColor(DIM_GREEN);
        lineNumbers.setAlignment(
            arc.util.Align.top | arc.util.Align.right
        );

        // Editor layout
        editorRows = new Table(Styles.black);
        editorRows.top();

        editorRows.add(lineNumbers)
            .width(42f)
            .top()
            .pad(8f);

        editorRows.add(codeArea)
            .growX()
            .top()
            .pad(8f);

        // Scroll the editor and line numbers together
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

        // Terminal / diagnostics panel
        Table terminal = new Table(
            Styles.grayPanelDark
        );

        Label terminalTitle = new Label(
            "TERMINAL / DIAGNOSTICS",
            Styles.monoLabel
        );

        terminalTitle.setColor(GREEN);

        diagnostics = new Label(
            "Editor ready. Validation pending.",
            Styles.monoLabel
        );

        diagnostics.setColor(LIGHT_GREEN);

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
            .row();

        // Update line numbers whenever code changes
        codeArea.addListener(new ChangeListener() {
            @Override
            public void changed(
                ChangeEvent event,
                Element element
            ) {
                refreshEditor();
            }
        });

        // Buttons
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

    private void refreshEditor() {

        String[] lines =
            codeArea.getText().split("\n", -1);

        int count = Math.max(1, lines.length);

        StringBuilder numbers = new StringBuilder();

        for (int i = 1; i <= count; i++) {
            numbers.append(i);

            if (i < count) {
                numbers.append('\n');
            }
        }

        lineNumbers.setText(numbers.toString());

        codeArea.setPrefRows(
            Math.max(18, count)
        );

        status.setText(
            "READY | " + count + " LINES"
        );

        editorRows.invalidateHierarchy();
        editorScroll.invalidateHierarchy();
    }

    public void showEditor(
        String currentCode,
        Cons<String> apply
    ) {
        codeArea.setText(currentCode);

        applyCode = apply;

        refreshEditor();

        diagnostics.setText(
            "Editor ready. Validation pending."
        );

        show();
    }
}
