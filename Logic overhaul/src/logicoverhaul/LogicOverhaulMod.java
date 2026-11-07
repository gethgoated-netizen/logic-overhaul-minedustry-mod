
package logicoverhaul;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.scene.ui.Label;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Log;

import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class LogicOverhaulMod extends Mod {

    public static final String THEME_SETTING =
        "logic-overhaul-editor-theme";

    public static final String DEBUG_KEY_SETTING =
        "logic-overhaul-debug-key";

    private static final String[][] THEMES = {
        {"classic", "Classic Terminal"},
        {"midnight", "Midnight Blue"},
        {"light", "Light"},
        {"contrast", "High Contrast"},
        {"base", "Base Game"},
        {"terminal", "Terminal"}
    };

    public static int cursorX;
    public static int cursorY;

    public static boolean mouseLeft;
    public static boolean mouseRight;

    public static boolean keyUp;
    public static boolean keyDown;
    public static boolean keyLeft;
    public static boolean keyRight;

    private TextLogicDialog textEditor;

    private Table hudRoot;
    private Table hudPanel;

    private Label cursorXLabel;
    private Label cursorYLabel;
    private Label mouseLeftLabel;
    private Label mouseRightLabel;

    private Label keyUpLabel;
    private Label keyDownLabel;
    private Label keyLeftLabel;
    private Label keyRightLabel;

    private boolean hudVisible;
    private boolean capturingKeybind;

    private KeyCode debugKey = KeyCode.slash;

    @Override
    public void init() {

        Core.settings.defaults(THEME_SETTING, "classic");
        Core.settings.defaults(DEBUG_KEY_SETTING, "slash");

        loadDebugKey();

        Events.run(Trigger.update, () -> {

            updateInputValues();
            updateMouseLogicVariables();
            updateHudValues();

            // Capture a replacement key before checking the toggle.
            if (capturingKeybind) {
                captureDebugKey();
                return;
            }

            if (Core.scene == null) return;

            // Do not trigger the HUD while typing or using a dialog.
            if (Core.scene.getKeyboardFocus() instanceof TextField) {
                return;
            }

            if (Core.scene.hasDialog()) {
                return;
            }

            if (Core.input.keyTap(debugKey)) {
                hudVisible = !hudVisible;

                if (hudRoot != null) {
                    hudRoot.visible = hudVisible;
                }
            }
        });

        Events.on(ClientLoadEvent.class, event -> {

            registerLogicVariables();
            addEditorSettings();
            createDebugHud();

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

    // ------------------------------------------------------------
    // Input tracking
    // ------------------------------------------------------------

    private void updateInputValues() {

        // Convert world coordinates into map tile coordinates.
        cursorX = (int)Math.floor(
            Core.input.mouseWorldX() / Vars.tilesize
        );

        cursorY = (int)Math.floor(
            Core.input.mouseWorldY() / Vars.tilesize
        );

        mouseLeft = Core.input.keyDown(KeyCode.mouseLeft);
        mouseRight = Core.input.keyDown(KeyCode.mouseRight);

        keyUp = Core.input.keyDown(KeyCode.up);
        keyDown = Core.input.keyDown(KeyCode.down);
        keyLeft = Core.input.keyDown(KeyCode.left);
        keyRight = Core.input.keyDown(KeyCode.right);
    }

    // ------------------------------------------------------------
    // Logic variables
    // ------------------------------------------------------------

    private void registerLogicVariables() {

        registerVariable("@cursorX");
        registerVariable("@cursorY");
        registerVariable("@mouseLeft");
        registerVariable("@mouseRight");

        registerVariable("@keyUp");
        registerVariable("@keyDown");
        registerVariable("@keyLeft");
        registerVariable("@keyRight");

        Log.info("[Logic Overhaul] Input variables registered!");
    }

    private void registerVariable(String name) {

        if (Vars.logicVars.get(name) == null) {
            Vars.logicVars.putEntry(name, 0);
        }
    }

    private void updateMouseLogicVariables() {

        if (Vars.logicVars == null) return;

        setVariable("@cursorX", cursorX);
        setVariable("@cursorY", cursorY);

        setVariable("@mouseLeft", mouseLeft ? 1 : 0);
        setVariable("@mouseRight", mouseRight ? 1 : 0);

        setVariable("@keyUp", keyUp ? 1 : 0);
        setVariable("@keyDown", keyDown ? 1 : 0);
        setVariable("@keyLeft", keyLeft ? 1 : 0);
        setVariable("@keyRight", keyRight ? 1 : 0);
    }

    private void setVariable(String name, double value) {

        if (Vars.logicVars.get(name) != null) {
            Vars.logicVars.set(name, value);
        }
    }

    // ------------------------------------------------------------
    // Debug HUD
    // ------------------------------------------------------------

    private void createDebugHud() {

        hudRoot = new Table();
        hudRoot.setFillParent(true);
        hudRoot.top().right();
        hudRoot.visible = false;

        hudPanel = new Table(Tex.button);
        hudPanel.defaults().pad(3f);

        hudPanel.add("LOGIC OVERHAUL DEBUG")
            .colspan(2)
            .left()
            .padBottom(6f)
            .row();

        cursorXLabel = addHudLine("Mouse X (tile)");
        cursorYLabel = addHudLine("Mouse Y (tile)");

        mouseLeftLabel = addHudLine("Left Click");
        mouseRightLabel = addHudLine("Right Click");

        keyUpLabel = addHudLine("Arrow Up");
        keyDownLabel = addHudLine("Arrow Down");
        keyLeftLabel = addHudLine("Arrow Left");
        keyRightLabel = addHudLine("Arrow Right");

        hudRoot.add(hudPanel)
            .top()
            .right()
            .pad(12f);

        Vars.ui.hudGroup.addChild(hudRoot);
    }

    private Label addHudLine(String title) {

        hudPanel.add(title).left();

        Label value = new Label("0", Styles.defaultLabel);

        hudPanel.add(value)
            .right()
            .minWidth(65f)
            .row();

        return value;
    }

    private void updateHudValues() {

        if (cursorXLabel == null) return;

        cursorXLabel.setText(String.valueOf(cursorX));
        cursorYLabel.setText(String.valueOf(cursorY));

        mouseLeftLabel.setText(mouseLeft ? "TRUE" : "FALSE");
        mouseRightLabel.setText(mouseRight ? "TRUE" : "FALSE");

        keyUpLabel.setText(keyUp ? "TRUE" : "FALSE");
        keyDownLabel.setText(keyDown ? "TRUE" : "FALSE");
        keyLeftLabel.setText(keyLeft ? "TRUE" : "FALSE");
        keyRightLabel.setText(keyRight ? "TRUE" : "FALSE");
    }

    // ------------------------------------------------------------
    // Debug HUD keybind
    // ------------------------------------------------------------

    private void loadDebugKey() {

        String saved = Core.settings.getString(
            DEBUG_KEY_SETTING,
            "slash"
        );

        try {
            debugKey = KeyCode.valueOf(saved);
        } catch (Exception ignored) {
            debugKey = KeyCode.slash;
        }
    }

    private void showDebugKeyDialog() {

        BaseDialog dialog =
            new BaseDialog("Debug HUD Keybind");

        dialog.cont.add("Current key: " + debugKey.name())
            .pad(8f)
            .row();

        dialog.cont.add(
            "Press the key you want to use."
        ).pad(8f).row();

        dialog.cont.button("Choose Key", () -> {
            capturingKeybind = true;
            dialog.hide();
            Vars.ui.showInfoToast(
                "Press a key to assign the Debug HUD toggle.",
                3f
            );
        }).size(240f, 50f).row();

        dialog.cont.button("Reset to /", () -> {
            debugKey = KeyCode.slash;
            Core.settings.put(DEBUG_KEY_SETTING, "slash");
            dialog.hide();
            Vars.ui.showInfoToast(
                "Debug HUD key reset to /",
                2f
            );
        }).size(240f, 50f).row();

        dialog.addCloseButton();
        dialog.show();
    }

    private void captureDebugKey() {

        for (KeyCode key : KeyCode.values()) {

            if (key == KeyCode.unknown) continue;
            if (key == KeyCode.escape) continue;

            if (Core.input.keyTap(key)) {

                debugKey = key;

                Core.settings.put(
                    DEBUG_KEY_SETTING,
                    key.name()
                );

                capturingKeybind = false;

                Vars.ui.showInfoToast(
                    "Debug HUD key set to " + key.name(),
                    3f
                );

                return;
            }
        }
    }

    // ------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------

    private void addEditorSettings() {

        Vars.ui.settings.addCategory("Logic Overhaul", table -> {

            table.add("Editor Appearance")
                .left()
                .padBottom(8f)
                .row();

            table.button(
                "Choose Editor Theme",
                this::showThemeDialog
            ).size(320f, 54f)
             .left()
             .row();

            table.add("Theme selection is saved automatically.")
                .width(380f)
                .wrap()
                .left()
                .padTop(5f)
                .row();

            table.add("Debug HUD")
                .left()
                .padTop(16f)
                .padBottom(8f)
                .row();

            table.button(
                "Change Debug HUD Key",
                this::showDebugKeyDialog
            ).size(320f, 54f)
             .left()
             .row();

            table.add(
                "Current key: " + debugKey.name()
            ).left().padTop(5f).row();

            table.add(
                "Press the assigned key to show or hide the HUD."
            ).width(380f)
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
