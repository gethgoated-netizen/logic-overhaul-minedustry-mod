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

        // Track mouse and update logic variables.
        Events.run(Trigger.update, () -> {

            cursorX = Core.input.mouseWorldX();
            cursorY = Core.input.mouseWorldY();

            mouseLeft = Core.input.keyDown(KeyCode.mouseLeft);
            mouseRight = Core.input.keyDown(KeyCode.mouseRight);

            updateMouseLogicVariables();
        });

        Events.on(ClientLoadEvent.class, event -> {

            Core.settings.defaults(THEME_SETTING, "classic");

            registerMouseLogicVariables();

            addEditorSettings();

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

    // Register variables with Mindustry's logic system.
    private void registerMouseLogicVariables() {

        registerVariable("@cursorX");
        registerVariable("@cursorY");
        registerVariable("@mouseLeft");
        registerVariable("@mouseRight");

        Log.info("[Logic Overhaul] Mouse logic variables registered!");
    }

    private void registerVariable(String name) {

        if (Vars.logicVars.get(name) == null) {
            Vars.logicVars.putEntry(name, 0);
        }
    }

    // Update their values continuously.
    private void updateMouseLogicVariables() {

        if (Vars.logicVars == null) return;

        setVariable("@cursorX", cursorX);
        setVariable("@cursorY", cursorY);

        setVariable("@mouseLeft", mouseLeft ? 1 : 0);
        setVariable("@mouseRight", mouseRight ? 1 : 0);
    }

    private void setVariable(String name, double value) {

        if (Vars.logicVars.get(name) != null) {
            Vars.logicVars.set(name, value);
        }
    }

    // Editor settings.
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

    private void showThemeDialog() {

        BaseDialog dialog =
            new BaseDialog("Logic Overhaul - Editor Themes");

        String selected =
            Core.settings.getString(THEME_SETTING, "classic");

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
