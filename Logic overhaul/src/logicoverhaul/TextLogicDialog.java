package logicoverhaul;

import arc.func.Cons;
import arc.scene.ui.TextArea;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;

public class TextLogicDialog extends BaseDialog {

    private TextArea codeArea;
    private Cons<String> applyCode;

    public TextLogicDialog() {
        super("Logic Overhaul - Text Editor");

        codeArea = new TextArea("");

        cont.pane(code -> {
            code.add(codeArea)
                .grow()
                .minSize(500f, 300f);
        }).grow();

        buttons.defaults().size(160f, 54f);

        buttons.button("Apply to Visual Editor", () -> {
            try {
                applyCode.get(codeArea.getText());
                hide();
            } catch (Throwable error) {
                Vars.ui.showException(error);
            }
        });

        buttons.button("Cancel", this::hide);
    }

    public void showEditor(String currentCode, Cons<String> apply) {
        codeArea.setText(currentCode);
        applyCode = apply;
        show();
    }
}