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

import java.util.Locale;

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
    public static boolean mouseMiddle;

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
    private Label mouseMiddleLabel;

    private Label keyUpLabel;
    private Label keyDownLabel;
    private Label keyLeftLabel;
    private Label keyRightLabel;

    private Label keysHeldLabel;

    private boolean hudVisible;
    private boolean capturingKeybind;

    private KeyCode debugKey = KeyCode.slash;

    @Override
    public void init() {

        Core.settings.defaults(THEME_SETTING, "classic");
        Core.settings.defaults(DEBUG_KEY_SETTING, "slash");

        loadDebugKey();

        // Update input and logic variables continuously.
        Events.run(Trigger.update, () -> {

            updateInputValues();
            updateLogicVariables();
            updateHudValues();

            if (capturingKeybind) {
                captureDebugKey();
                return;
            }

            if (Core.scene == null) return;

            // Don't toggle the HUD while typing or using dialogs.
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
    // KEY HELPERS
    // ------------------------------------------------------------

    // Only use actual keyboard-type codes.
    // Excludes the special aggregate and unknown entries.
    public static boolean isKeyboardKey(KeyCode key) {

        return key.type == KeyCode.KeyType.key
            && key != KeyCode.anyKey
            && key != KeyCode.unknown;
    }

    // Converts Arc key names into Logic Overhaul variable names.
    // Examples: a -> @keyA, shiftLeft -> @keyShiftLeft.
    public static String keyVariableName(KeyCode key) {

        String name = key.name();

        if (name.length() == 1) {
            name = name.toUpperCase(Locale.ROOT);
        } else {
            name = Character.toUpperCase(name.charAt(0))
                + name.substring(1);
        }

        return "@key" + name;
    }

    // ------------------------------------------------------------
    // INPUT TRACKING
    // ------------------------------------------------------------

    private void updateInputValues() {

        // Mindustry's map tile conversion uses nearest-tile rounding.
        cursorX = Math.round(
            Core.input.mouseWorldX() / Vars.tilesize
        );

        cursorY = Math.round(
            Core.input.mouseWorldY() / Vars.tilesize
        );

        mouseLeft = Core.input.keyDown(KeyCode.mouseLeft);
        mouseRight = Core.input.keyDown(KeyCode.mouseRight);
        mouseMiddle = Core.input.keyDown(KeyCode.mouseMiddle);

        keyUp = Core.input.keyDown(KeyCode.up);
        keyDown = Core.input.keyDown(KeyCode.down);
        keyLeft = Core.input.keyDown(KeyCode.left);
        keyRight = Core.input.keyDown(KeyCode.right);
    }

    // ------------------------------------------------------------
    // LOGIC VARIABLE REGISTRATION
    // ------------------------------------------------------------

    private void registerLogicVariables() {

        registerVariable("@cursorX");
        registerVariable("@cursorY");

        registerVariable("@mouseLeft");
        registerVariable("@mouseRight");
        registerVariable("@mouseMiddle");

        registerVariable("@keyAny");

        // Register every keyboard key exposed by Arc.
        for (KeyCode key : KeyCode.all) {

            if (!isKeyboardKey(key)) continue;

            registerVariable(keyVariableName(key));
        }

        Log.info(
            "[Logic Overhaul] Mouse and keyboard variables registered!"
        );
    }

    private void registerVariable(String name) {

        if (Vars.logicVars.get(name) == null) {
            Vars.logicVars.putEntry(name, 0);
        }
    }

    // ------------------------------------------------------------
    // LOGIC VARIABLE UPDATES
    // ------------------------------------------------------------

    private void updateLogicVariables() {

        if (Vars.logicVars == null) return;

        setVariable("@cursorX", cursorX);
        setVariable("@cursorY", cursorY);

        setVariable("@mouseLeft", mouseLeft ? 1 : 0);
        setVariable("@mouseRight", mouseRight ? 1 : 0);
        setVariable("@mouseMiddle", mouseMiddle ? 1 : 0);

        setVariable("@keyUp", keyUp ? 1 : 0);
        setVariable("@keyDown", keyDown ? 1 : 0);
        setVariable("@keyLeft", keyLeft ? 1 : 0);
        setVariable("@keyRight", keyRight ? 1 : 0);

        boolean anyKeyboardKeyPressed = false;

        // Update every exposed keyboard variable.
        for (KeyCode key : KeyCode.all) {

            if (!isKeyboardKey(key)) continue;

            boolean pressed = Core.input.keyDown(key);

            setVariable(
                keyVariableName(key),
                pressed ? 1 : 0
            );

            if (pressed) {
                anyKeyboardKeyPressed = true;
            }
        }

        setVariable(
            "@keyAny",
            anyKeyboardKeyPressed ? 1 : 0
        );
    }

    private void setVariable(String name, double value) {

        if (Vars.logicVars.get(name) != null) {
            Vars.logicVars.set(name, value);
        }
    }

    // ------------------------------------------------------------
    // DEBUG HUD
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
        mouseMiddleLabel = addHudLine("Middle Click");

        keyUpLabel = addHudLine("Arrow Up");
        keyDownLabel = addHudLine("Arrow Down");
        keyLeftLabel = addHudLine("Arrow Left");
        keyRightLabel = addHudLine("Arrow Right");

        hudPanel.add("Keys Held")
            .left()
            .top();

        keysHeldLabel = new Label(
            "None",
            Styles.defaultLabel
        );

        keysHeldLabel.setWrap(true);

        hudPanel.add(keysHeldLabel)
            .width(180f)
            .right()
            .row();

        hudRoot.add(hudPanel)
            .top()
            .right()
            .pad(12f);

        Vars.ui.hudGroup.addChild(hudRoot);
    }

    private Label addHudLine(String title) {

        hudPanel.add(title).left();

        Label value = new Label(
            "FALSE",
            Styles.defaultLabel
        );

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
        mouseMiddleLabel.setText(mouseMiddle ? "TRUE" : "FALSE");

        keyUpLabel.setText(keyUp ? "TRUE" : "FALSE");
        keyDownLabel.setText(keyDown ? "TRUE" : "FALSE");
        keyLeftLabel.setText(keyLeft ? "TRUE" : "FALSE");
        keyRightLabel.setText(keyRight ? "TRUE" : "FALSE");

        StringBuilder held = new StringBuilder();

        for (KeyCode key : KeyCode.all) {

            if (!isKeyboardKey(key)) continue;
            if (!Core.input.keyDown(key)) continue;

            if (held.length() > 0) {
                held.append(", ");
            }

            held.append(key.getName());
        }

        keysHeldLabel.setText(
            held.length() == 0 ? "None" : held.toString()
        );
    }

    // ------------------------------------------------------------
    // DEBUG HUD KEYBIND
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

        dialog.cont.add(
            "Current key: " + debugKey.getName()
        )
        .pad(8f)
        .row();

        dialog.cont.add(
            "Press the key you want to use."
        )
        .pad(8f)
        .row();

        dialog.cont.button("Choose Key", () -> {

            capturingKeybind = true;

            dialog.hide();

            Vars.ui.showInfoToast(
                "Press a keyboard key to assign the HUD toggle.",
                3f
            );

        }).size(240f, 50f).row();

        dialog.cont.button("Reset to /", () -> {

            debugKey = KeyCode.slash;

            Core.settings.put(
                DEBUG_KEY_SETTING,
                "slash"
            );

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

        if (Core.input.keyTap(KeyCode.escape)) {

            capturingKeybind = false;

            Vars.ui.showInfoToast(
                "Keybind change cancelled.",
                2f
            );

            return;
        }

        for (KeyCode key : KeyCode.all) {

            if (!isKeyboardKey(key)) continue;

            if (Core.input.keyTap(key)) {

                debugKey = key;

                Core.settings.put(
                    DEBUG_KEY_SETTING,
                    key.name()
                );

                capturingKeybind = false;

                Vars.ui.showInfoToast(
                    "Debug HUD key set to " + key.getName(),
                    3f
                );

                return;
            }
        }
    }

    // ------------------------------------------------------------
    // SETTINGS
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
            )
            .size(320f, 54f)
            .left()
            .row();

            table.add(
                "Theme selection is saved automatically."
            )
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
            )
            .size(320f, 54f)
            .left()
            .row();

            table.add(
                "Current key: " + debugKey.getName()
            )
            .left()
            .padTop(5f)
            .row();

            table.add(
                "Press the assigned key to show or hide the HUD."
            )
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
