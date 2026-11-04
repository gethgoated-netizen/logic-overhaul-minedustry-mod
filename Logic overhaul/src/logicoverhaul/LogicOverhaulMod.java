package logicoverhaul;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.util.Log;

import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.ui.dialogs.BaseDialog;

public class LogicOverhaulMod extends Mod {

    public static final String THEME_SETTING =
        "logic-overhaul-editor-theme";

    private static final String[][] THEMES = {
        {"classic", "Classic Terminal"},
        {"midnight", "Midnight Blue"},
        {"light", "Light"},
        {"contrast", "High Contrast"},
        {"base", "Base Game"},
        {"terminal", "Terminal"}
    };

    public static float cursorX;
    public static float cursorY;

    public static boolean mouseLeft;
    public static boolean mouseRight;

    private TextLogicDialog textEditor;

    @Override
    public void init() {

        // Preserve existing mouse and cursor tracking.
        Events.run(Trigger.update, () -> {
            cursorX = Core.input.mouseWorldX();
            cursorY = Core.input.mouseWorldY();

            mouseLeft = Core.input.keyDown(KeyCode.mouseLeft);
            mouseRight = Core.input.keyDown(KeyCode.mouseRight);
        });

        Events.on(ClientLoadEvent.class, event -> {

            Core.settings.defaults(THEME_SETTING, "classic");

            addEditorSettings();

            // Preserve the existing text editor connection.
            textEditor = new TextLogicDialog();

            Vars.ui.logic.shown(() -> {

                Vars.ui.logic.buttons.button("Text Editor", () -> {

                    textEditor.showEditor(
                        Vars.ui.logic.canvas.save(),
                        code -> Vars.ui.logic.canvas.load(code)
                    );

                }).size(160f, 64f)
                  .name("logic-overhaul-text");
            });

            Log.info("[Logic Overhaul] Loaded successfully!");
        });
    }

    // Editor settings category.
    private void addEditorSettings() {

        Vars.ui.settings.addCategory("Logic Overhaul", table -> {

            table.add("Editor Appearance")
                .left()
                .padBottom(8f)
                .row();

            table.button(
                "Choose Editor Theme",
                this::showThemeDialog
            )
            .size(320f, 54f)
            .left()
            .row();

            table.add("Theme selection is saved automatically.")
                .width(380f)
                .wrap()
                .left()
                .padTop(5f);
        });
    }

    // Theme selection dialog.
    private void showThemeDialog() {

        BaseDialog dialog =
            new BaseDialog("Logic Overhaul - Editor Themes");

        String selected =
            Core.settings.getString(THEME_SETTING, "classic");

        dialog.cont.add("Choose your preferred editor theme.")
            .left()
            .padBottom(10f)
            .row();

        for (String[] theme : THEMES) {

            String id = theme[0];
            String name = theme[1];

            String label = id.equals(selected)
                ? name + " (Selected)"
                : name;

            dialog.cont.button(label, () -> {

                Core.settings.put(THEME_SETTING, id);

                Vars.ui.showInfoToast(
                    "Saved theme: " + name,
                    2f
                );

                dialog.hide();

            }).size(320f, 48f)
              .pad(3f)
              .row();
        }

        dialog.addCloseButton();
        dialog.show();
    }
}
