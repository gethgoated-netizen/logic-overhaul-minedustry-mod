package logicoverhaul;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.util.Log;
import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.ClientLoadEvent;

public class LogicOverhaulMod extends Mod {

    public static float cursorX;
    public static float cursorY;

    public static boolean mouseLeft;
    public static boolean mouseRight;

    private TextLogicDialog textEditor;

    @Override
    public void init() {

        // Track mouse input
        Events.run(Trigger.update, () -> {
            cursorX = Core.input.mouseWorldX();
            cursorY = Core.input.mouseWorldY();

            mouseLeft = Core.input.keyDown(KeyCode.mouseLeft);
            mouseRight = Core.input.keyDown(KeyCode.mouseRight);
        });

        // Add our editor to the Logic menu
        Events.on(ClientLoadEvent.class, event -> {

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
}