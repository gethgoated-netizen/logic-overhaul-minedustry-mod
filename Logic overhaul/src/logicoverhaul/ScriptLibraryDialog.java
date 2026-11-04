package logicoverhaul;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.func.Prov;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextArea;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;

import mindustry.Vars;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public class ScriptLibraryDialog extends BaseDialog {

    private static final String PACKAGE_HEADER =
        "LOGIC_OVERHAUL_SCRIPT_V1";

    private final Fi directory;

    private final Cons<String> replaceCode;
    private final Cons<String> insertCode;
    private final Prov<String> getCurrentCode;

    private final Table scriptList = new Table();

    public ScriptLibraryDialog(
        Cons<String> replaceCode,
        Cons<String> insertCode,
        Prov<String> getCurrentCode
    ) {
        super("Logic Overhaul - Script Library");

        this.replaceCode = replaceCode;
        this.insertCode = insertCode;
        this.getCurrentCode = getCurrentCode;

        directory = Vars.dataDirectory
            .child("logic-overhaul")
            .child("scripts");

        directory.mkdirs();

        buildInterface();
    }

    // ================================================
    // MAIN LIBRARY
    // ================================================

    private void buildInterface() {

        cont.clear();

        cont.add("LOGIC OVERHAUL SCRIPT LIBRARY")
            .left()
            .pad(8f)
            .row();

        cont.add(
            "Scripts are stored locally on this device."
        )
        .left()
        .padBottom(10f)
        .row();

        ScrollPane scroll = new ScrollPane(
            scriptList,
            Styles.noBarPane
        );

        scroll.setFadeScrollBars(false);

        cont.add(scroll)
            .grow()
            .minSize(600f, 320f)
            .row();

        cont.button(
            "Save Current Script",
            this::saveCurrent
        )
        .size(190f, 48f);

        cont.button(
            "Import Script",
            this::showImport
        )
        .size(150f, 48f);

        cont.button(
            "Refresh",
            this::refresh
        )
        .size(120f, 48f);

        buttons.button("Close", this::hide);

        refresh();
    }

    private void saveCurrent() {

        String source = getCurrentCode.get();

        showSaveDialog(
            source == null ? "" : source
        );
    }

    // ================================================
    // SAVE DIALOG
    // ================================================

    public void showSaveDialog(String source) {

        BaseDialog dialog = new BaseDialog("Save Script");

        TextField name = new TextField("");
        TextField category = new TextField("My Scripts");
        TextArea description = new TextArea("");

        String author = getCurrentAuthor();

        dialog.cont.add("Script Name")
            .left()
            .row();

        dialog.cont.add(name)
            .width(450f)
            .row();

        dialog.cont.add("Category")
            .left()
            .row();

        dialog.cont.add(category)
            .width(450f)
            .row();

        dialog.cont.add("Author: " + author)
            .left()
            .padTop(8f)
            .row();

        dialog.cont.add("Your Description")
            .left()
            .padTop(8f)
            .row();

        dialog.cont.add(description)
            .width(450f)
            .height(120f)
            .row();

        dialog.cont.add(
            "Write your own explanation of what the script does."
        )
        .left()
        .row();

        dialog.buttons.button("Save", () -> {

            String scriptName = cleanName(name.getText());
            String scriptCategory = cleanName(category.getText());

            if (scriptName.isEmpty()) {
                Vars.ui.showInfoToast(
                    "Enter a script name.",
                    2f
                );
                return;
            }

            if (scriptCategory.isEmpty()) {
                scriptCategory = "My Scripts";
            }

            ScriptData data = new ScriptData();

            data.name = scriptName;
            data.category = scriptCategory;
            data.author = getCurrentAuthor();
            data.description = description.getText();
            data.code = source == null ? "" : source;

            saveScript(data, () -> dialog.hide());
        });

        dialog.buttons.button("Cancel", dialog::hide);

        dialog.show();
    }

    // ================================================
    // AUTOMATIC AUTHOR
    // ================================================

    private String getCurrentAuthor() {

        String author = Core.settings.getString("name", "");

        if (author == null || author.trim().isEmpty()) {
            return "Anonymous User";
        }

        return author.trim();
    }

    // ================================================
    // SAVE SCRIPT
    // ================================================

    private void saveScript(
        ScriptData data,
        Runnable afterSave
    ) {

        data.name = cleanName(data.name);
        data.category = cleanName(data.category);

        if (data.name.isEmpty()) {
            Vars.ui.showInfoToast(
                "Invalid script name.",
                2f
            );
            return;
        }

        if (data.category.isEmpty()) {
            data.category = "My Scripts";
        }

        Fi folder = directory.child(data.category);
        folder.mkdirs();

        data.file = folder.child(data.name + ".mlog");

        if (data.file.exists()) {

            BaseDialog confirm =
                new BaseDialog("Overwrite Script?");

            confirm.cont.add(
                "A script with this name already exists."
            );

            confirm.buttons.button("Overwrite", () -> {

                if (writeScript(data)) {
                    confirm.hide();
                    refresh();

                    if (afterSave != null) {
                        afterSave.run();
                    }
                }

            });

            confirm.buttons.button("Cancel", confirm::hide);

            confirm.show();

            return;
        }

        if (writeScript(data)) {

            refresh();

            if (afterSave != null) {
                afterSave.run();
            }
        }
    }

    private boolean writeScript(ScriptData data) {

        try {

            data.file.writeString(
                data.code == null ? "" : data.code
            );

            writeMetadata(data);

            Vars.ui.showInfoToast(
                "Script saved successfully.",
                2f
            );

            return true;

        } catch (Throwable error) {

            Vars.ui.showException(error);
            return false;
        }
    }

    // ================================================
    // SAVE SCRIPT INFORMATION
    // ================================================

    private void writeMetadata(ScriptData data) {

        try {

            Properties properties = new Properties();

            properties.setProperty("name", data.name);
            properties.setProperty("category", data.category);
            properties.setProperty("author", data.author);
            properties.setProperty(
                "description",
                data.description == null ? "" : data.description
            );

            StringWriter writer = new StringWriter();

            properties.store(
                writer,
                "Logic Overhaul Script Information"
            );

            getMetadataFile(data.file).writeString(
                writer.toString()
            );

        } catch (Throwable error) {

            Vars.ui.showException(error);
        }
    }

    private ScriptData readScript(Fi file) {

        ScriptData data = new ScriptData();

        data.file = file;
        data.code = file.readString();

        data.name = file.nameWithoutExtension();
        data.category = file.parent().name();
        data.author = "Anonymous User";
        data.description = "";

        Fi metadata = getMetadataFile(file);

        if (!metadata.exists()) {
            return data;
        }

        try {

            Properties properties = new Properties();

            properties.load(
                new StringReader(metadata.readString())
            );

            data.name = properties.getProperty(
                "name",
                data.name
            );

            data.category = properties.getProperty(
                "category",
                data.category
            );

            data.author = properties.getProperty(
                "author",
                data.author
            );

            data.description = properties.getProperty(
                "description",
                ""
            );

        } catch (Throwable ignored) {

            // Older scripts without metadata still load.
        }

        return data;
    }

    private Fi getMetadataFile(Fi file) {

        return file.parent().child(
            file.nameWithoutExtension() + ".meta"
        );
    }

    // ================================================
    // REFRESH SCRIPT LIST
    // ================================================

    private void refresh() {

        scriptList.clear();

        directory.mkdirs();

        List<Fi> files = new ArrayList<>();

        findScripts(directory, files);

        Collections.sort(
            files,
            Comparator.comparing(
                Fi::name,
                String.CASE_INSENSITIVE_ORDER
            )
        );

        if (files.isEmpty()) {

            scriptList.add("No saved scripts yet.")
                .left()
                .pad(12f);

            return;
        }

        for (Fi file : files) {

            ScriptData data = readScript(file);

            scriptList.table(row -> {

                row.add(data.name)
                    .width(150f)
                    .left();

                row.add(data.category)
                    .width(100f)
                    .left();

                row.add("By " + data.author)
                    .width(160f)
                    .left();

                row.button(
                    "Open",
                    () -> showPreview(data)
                )
                .width(65f);

                row.button(
                    "Delete",
                    () -> confirmDelete(file)
                )
                .width(65f);

                row.row();

                String description =
                    data.description == null ||
                    data.description.trim().isEmpty()
                    ? "No description provided."
                    : data.description;

                row.add(description)
                    .colspan(5)
                    .growX()
                    .left()
                    .padBottom(12f);

            }).growX();

            scriptList.row();
        }
    }

    private void findScripts(
        Fi folder,
        List<Fi> result
    ) {

        for (Fi file : folder.list()) {

            if (file.isDirectory()) {

                findScripts(file, result);

            } else if (
                file.extension().equalsIgnoreCase("mlog")
            ) {

                result.add(file);
            }
        }
    }

    // ================================================
    // SCRIPT PREVIEW
    // ================================================

    private void showPreview(ScriptData data) {

        BaseDialog preview = new BaseDialog(data.name);

        preview.cont.add("Category: " + data.category)
            .left()
            .row();

        preview.cont.add("Author: " + data.author)
            .left()
            .row();

        preview.cont.add("Description")
            .left()
            .padTop(8f)
            .row();

        preview.cont.add(
            data.description.trim().isEmpty()
                ? "No description provided."
                : data.description
        )
        .width(580f)
        .left()
        .padBottom(8f)
        .row();

        TextArea code = new TextArea(data.code);

        preview.cont.add(code)
            .width(580f)
            .height(300f);

        preview.buttons.button(
            "Replace Editor",
            () -> {

                replaceCode.get(data.code);

                preview.hide();
                hide();
            }
        );

        preview.buttons.button(
            "Insert at Cursor",
            () -> {

                insertCode.get(data.code);

                preview.hide();
                hide();
            }
        );

        preview.buttons.button(
            "Edit Description",
            () -> showEditDescription(data, preview)
        );

        preview.buttons.button(
            "Export",
            () -> showExport(data)
        );

        preview.buttons.button(
            "Close",
            preview::hide
        );

        preview.show();
    }

    // ================================================
    // EDIT DESCRIPTION
    // ================================================

    private void showEditDescription(
        ScriptData data,
        BaseDialog oldPreview
    ) {

        BaseDialog edit =
            new BaseDialog("Edit Script Description");

        TextArea description =
            new TextArea(data.description);

        edit.cont.add("Write your new description.")
            .left()
            .row();

        edit.cont.add(description)
            .width(500f)
            .height(160f);

        edit.buttons.button("Save", () -> {

            data.description = description.getText();

            writeMetadata(data);

            edit.hide();
            oldPreview.hide();

            refresh();

            showPreview(readScript(data.file));
        });

        edit.buttons.button("Cancel", edit::hide);

        edit.show();
    }

    // ================================================
    // DELETE SCRIPT
    // ================================================

    private void confirmDelete(Fi file) {

        BaseDialog confirm =
            new BaseDialog("Delete Script?");

        confirm.cont.add(
            "Are you sure you want to delete " +
            file.nameWithoutExtension() + "?"
        );

        confirm.buttons.button("Delete", () -> {

            file.delete();
            getMetadataFile(file).delete();

            confirm.hide();

            refresh();
        });

        confirm.buttons.button("Cancel", confirm::hide);

        confirm.show();
    }

    // ================================================
    // EXPORT SCRIPT
    // ================================================

    private void showExport(ScriptData data) {

        BaseDialog export =
            new BaseDialog("Export Script");

        TextArea packageText =
            new TextArea(createPackage(data));

        export.cont.add(
            "Copy this complete package to share your script."
        )
        .left()
        .row();

        export.cont.add(packageText)
            .width(600f)
            .height(330f);

        export.buttons.button("Close", export::hide);

        export.show();
    }

    private String createPackage(ScriptData data) {

        StringBuilder result = new StringBuilder();

        result.append(PACKAGE_HEADER).append("\n");

        result.append("name=")
            .append(encode(data.name))
            .append("\n");

        result.append("category=")
            .append(encode(data.category))
            .append("\n");

        result.append("author=")
            .append(encode(data.author))
            .append("\n");

        result.append("description=")
            .append(encode(data.description))
            .append("\n");

        result.append("code=")
            .append(encode(data.code));

        return result.toString();
    }

    // ================================================
    // IMPORT SCRIPT
    // ================================================

    private void showImport() {

        BaseDialog dialog =
            new BaseDialog("Import Script");

        TextField name = new TextField("");
        TextField category = new TextField("My Scripts");
        TextField author = new TextField(getCurrentAuthor());

        TextArea description = new TextArea("");
        TextArea code = new TextArea("");

        dialog.cont.add("Script Name")
            .left()
            .row();

        dialog.cont.add(name)
            .width(480f)
            .row();

        dialog.cont.add("Category")
            .left()
            .row();

        dialog.cont.add(category)
            .width(480f)
            .row();

        dialog.cont.add("Original Author")
            .left()
            .row();

        dialog.cont.add(author)
            .width(480f)
            .row();

        dialog.cont.add("Description")
            .left()
            .row();

        dialog.cont.add(description)
            .width(480f)
            .height(90f)
            .row();

        dialog.cont.add(
            "Paste an exported package or raw Mindustry logic."
        )
        .left()
        .row();

        dialog.cont.add(code)
            .width(600f)
            .height(230f);

        dialog.buttons.button("Import", () -> {

            String pasted = code.getText();

            if (pasted.trim().isEmpty()) {

                Vars.ui.showInfoToast(
                    "Paste a script first.",
                    2f
                );

                return;
            }

            ScriptData data;

            try {

                data = parsePackage(pasted);

            } catch (Throwable error) {

                Vars.ui.showInfoToast(
                    "Invalid script package.",
                    3f
                );

                return;
            }

            // Raw .mlog code: use the information entered.
            if (data == null) {

                data = new ScriptData();

                data.name = cleanName(name.getText());
                data.category = cleanName(category.getText());
                data.author = author.getText().trim();
                data.description = description.getText();
                data.code = pasted;

                if (data.name.isEmpty()) {

                    Vars.ui.showInfoToast(
                        "Enter a script name.",
                        2f
                    );

                    return;
                }

                if (data.category.isEmpty()) {
                    data.category = "My Scripts";
                }

                if (data.author.isEmpty()) {
                    data.author = "Anonymous User";
                }
            }

            // Keep original package author information.
            if (
                data.name == null ||
                data.name.trim().isEmpty()
            ) {

                Vars.ui.showInfoToast(
                    "The package has no script name.",
                    2f
                );

                return;
            }

            if (
                data.category == null ||
                data.category.trim().isEmpty()
            ) {
                data.category = "My Scripts";
            }

            if (
                data.author == null ||
                data.author.trim().isEmpty()
            ) {
                data.author = "Anonymous User";
            }

            data.name = cleanName(data.name);
            data.category = cleanName(data.category);

            saveScript(data, () -> dialog.hide());
        });

        dialog.buttons.button("Cancel", dialog::hide);

        dialog.show();
    }

    private ScriptData parsePackage(String text) {

        String[] lines = text.split("\\r?\\n");

        if (
            lines.length == 0 ||
            !PACKAGE_HEADER.equals(lines[0].trim())
        ) {
            return null;
        }

        Properties properties = new Properties();

        for (int i = 1; i < lines.length; i++) {

            int separator = lines[i].indexOf('=');

            if (separator <= 0) continue;

            String key = lines[i].substring(0, separator);

            String value = lines[i].substring(separator + 1);

            properties.setProperty(key, value);
        }

        ScriptData data = new ScriptData();

        data.name = decode(properties.getProperty("name", ""));
        data.category = decode(properties.getProperty("category", ""));
        data.author = decode(properties.getProperty("author", ""));
        data.description = decode(properties.getProperty("description", ""));
        data.code = decode(properties.getProperty("code", ""));

        return data;
    }

    // ================================================
    // PACKAGE ENCODING
    // ================================================

    private String encode(String value) {

        if (value == null) {
            value = "";
        }

        return Base64.getEncoder().encodeToString(
            value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String decode(String value) {

        return new String(
            Base64.getDecoder().decode(value),
            StandardCharsets.UTF_8
        );
    }

    // ================================================
    // CLEAN FILE NAMES
    // ================================================

    private String cleanName(String name) {

        if (name == null) {
            return "";
        }

        return name.trim()
            .replaceAll("[\\\\/:*?\"<>|]", "_")
            .replaceAll("^[. ]+", "")
            .replaceAll("[. ]+$", "");
    }

    // ================================================
    // SCRIPT DATA
    // ================================================

    private static class ScriptData {

        String name = "";
        String category = "My Scripts";
        String author = "Anonymous User";
        String description = "";
        String code = "";

        Fi file;
    }

    // ================================================
    // OPEN LIBRARY
    // ================================================

    @Override
    public void show() {

        refresh();

        super.show();
    }
}
