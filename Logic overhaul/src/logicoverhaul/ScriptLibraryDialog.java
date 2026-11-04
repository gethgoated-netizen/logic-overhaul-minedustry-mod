package logicoverhaul;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.func.Prov;
import arc.scene.ui.TextArea;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
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

    private static final String PACKAGE_HEADER = "LOGIC_OVERHAUL_SCRIPT_V1";

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

    // --------------------------------------------------
    // MAIN LIBRARY WINDOW
    // --------------------------------------------------

    private void buildInterface() {
        cont.clear();

        cont.add("Your saved scripts").left();
        cont.row();

        cont.add("Scripts are stored locally on this device.")
            .left()
            .padBottom(10f);

        cont.row();

        cont.pane(scriptList)
            .width(720f)
            .height(390f)
            .growX();

        cont.row();

        cont.button("Save Current Script", this::saveCurrent)
            .width(210f)
            .height(48f);

        cont.button("Import Script", this::showImport)
            .width(170f)
            .height(48f);

        cont.button("Refresh", this::refresh)
            .width(120f)
            .height(48f);

        buttons.button("Close", this::hide);

        refresh();
    }

    private void saveCurrent() {
        String source = getCurrentCode.get();

        if (source == null) {
            source = "";
        }

        showSaveDialog(source);
    }

    // --------------------------------------------------
    // SAVE DIALOG
    // --------------------------------------------------

    public void showSaveDialog(String source) {
        BaseDialog saveDialog = new BaseDialog("Save Script");

        TextField name = new TextField("");
        TextField category = new TextField("My Scripts");
        TextArea description = new TextArea("");

        String author = getCurrentAuthor();

        saveDialog.cont.add("Script Name").left();
        saveDialog.cont.row();

        saveDialog.cont.add(name)
            .width(430f)
            .left();

        saveDialog.cont.row();

        saveDialog.cont.add("Category").left();
        saveDialog.cont.row();

        saveDialog.cont.add(category)
            .width(430f)
            .left();

        saveDialog.cont.row();

        saveDialog.cont.add("Author: " + author)
            .left();

        saveDialog.cont.row();

        saveDialog.cont.add("Your Description").left();
        saveDialog.cont.row();

        saveDialog.cont.add(description)
            .width(430f)
            .height(120f)
            .left();

        saveDialog.cont.row();

        saveDialog.cont.add(
            "Write your own explanation of what this script does."
        ).left();

        saveDialog.buttons.button("Save", () -> {
            String scriptName = cleanName(name.getText());
            String scriptCategory = cleanName(category.getText());

            if (scriptName.isEmpty()) {
                Vars.ui.showInfoToast("Enter a script name.", 2f);
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

            saveScript(data);
            saveDialog.hide();
        });

        saveDialog.buttons.button("Cancel", saveDialog::hide);

        saveDialog.show();
    }

    // --------------------------------------------------
    // PLAYER AUTHOR
    // --------------------------------------------------

    private String getCurrentAuthor() {
        String author = "";

        if (Vars.player != null && Vars.player.name != null) {
            author = Vars.player.name;
        }

        if (author.trim().isEmpty()) {
            author = Core.settings.getString("name");
        }

        if (author == null || author.trim().isEmpty()) {
            return "Anonymous User";
        }

        return author.trim();
    }

    // --------------------------------------------------
    // SAVE SCRIPT FILES
    // --------------------------------------------------

    private void saveScript(ScriptData data) {
        Fi folder = directory.child(cleanName(data.category));
        folder.mkdirs();

        Fi file = folder.child(cleanName(data.name) + ".mlog");

        data.file = file;

        if (file.exists()) {
            BaseDialog confirm = new BaseDialog("Overwrite Script?");

            confirm.cont.add(
                "A script with this name already exists in this category."
            );

            confirm.buttons.button("Overwrite", () -> {
                writeScript(data);
                confirm.hide();
                refresh();
            });

            confirm.buttons.button("Cancel", confirm::hide);

            confirm.show();
            return;
        }

        writeScript(data);
        refresh();
    }

    private void writeScript(ScriptData data) {
        try {
            data.file.writeString(data.code == null ? "" : data.code);

            writeMetadata(data);

            Vars.ui.showInfoToast("Script saved.", 2f);
        } catch (Throwable error) {
            Vars.ui.showException(error);
        }
    }

    // --------------------------------------------------
    // SCRIPT METADATA
    // --------------------------------------------------

    private void writeMetadata(ScriptData data) {
        try {
            Properties properties = new Properties();

            properties.setProperty("name", data.name);
            properties.setProperty("category", data.category);
            properties.setProperty("author", data.author);
            properties.setProperty("description", data.description);

            StringWriter writer = new StringWriter();
            properties.store(writer, "Logic Overhaul Script Information");

            Fi metadataFile = getMetadataFile(data.file);
            metadataFile.writeString(writer.toString());
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

        Fi metadataFile = getMetadataFile(file);

        if (!metadataFile.exists()) {
            return data;
        }

        try {
            Properties properties = new Properties();
            properties.load(new StringReader(metadataFile.readString()));

            data.name = properties.getProperty("name", data.name);
            data.category = properties.getProperty("category", data.category);
            data.author = properties.getProperty("author", data.author);
            data.description = properties.getProperty("description", "");
        } catch (Throwable ignored) {
            // Older or damaged metadata will not prevent loading the script.
        }

        return data;
    }

    private Fi getMetadataFile(Fi scriptFile) {
        return scriptFile.parent().child(
            scriptFile.nameWithoutExtension() + ".meta"
        );
    }

    // --------------------------------------------------
    // LIST SAVED SCRIPTS
    // --------------------------------------------------

    private void refresh() {
        scriptList.clear();

        directory.mkdirs();

        List<Fi> files = new ArrayList<>();

        findScripts(directory, files);

        Collections.sort(files, Comparator.comparing(
            Fi::name,
            String.CASE_INSENSITIVE_ORDER
        ));

        if (files.isEmpty()) {
            scriptList.add("No scripts saved yet.")
                .left()
                .pad(12f);

            return;
        }

        for (Fi file : files) {
            ScriptData data = readScript(file);

            scriptList.table(row -> {
                row.add(data.name)
                    .width(180f)
                    .left();

                row.add(data.category)
                    .width(110f)
                    .left();

                row.add("By " + data.author)
                    .width(170f)
                    .left();

                row.button("Open", () -> showPreview(data))
                    .width(75f);

                row.button("Delete", () -> confirmDelete(file, null))
                    .width(75f);

                row.row();

                String description = data.description.trim().isEmpty()
                    ? "No description provided."
                    : data.description;

                row.add(description)
                    .colspan(5)
                    .growX()
                    .left()
                    .padBottom(10f);
            }).growX();

            scriptList.row();
        }
    }

    private void findScripts(Fi folder, List<Fi> result) {
        for (Fi file : folder.list()) {
            if (file.isDirectory()) {
                findScripts(file, result);
            } else if (file.extension().equalsIgnoreCase("mlog")) {
                result.add(file);
            }
        }
    }

    // --------------------------------------------------
    // SCRIPT PREVIEW
    // --------------------------------------------------

    private void showPreview(ScriptData data) {
        BaseDialog preview = new BaseDialog(data.name);

        preview.cont.add("Category: " + data.category).left();
        preview.cont.row();

        preview.cont.add("Author: " + data.author).left();
        preview.cont.row();

        preview.cont.add("Description").left();
        preview.cont.row();

        preview.cont.add(
            data.description.trim().isEmpty()
                ? "No description provided."
                : data.description
        ).width(580f).left();

        preview.cont.row();

        TextArea code = new TextArea(data.code);

        preview.cont.add(code)
            .width(580f)
            .height(300f);

        preview.buttons.button("Replace Editor", () -> {
            replaceCode.get(data.code);
            preview.hide();
            hide();
        });

        preview.buttons.button("Insert at Cursor", () -> {
            insertCode.get(data.code);
            preview.hide();
            hide();
        });

        preview.buttons.button("Edit Description", () -> {
            showEditDescription(data, preview);
        });

        preview.buttons.button("Export", () -> {
            showExport(data);
        });

        preview.buttons.button("Close", preview::hide);

        preview.show();
    }

    // --------------------------------------------------
    // EDIT DESCRIPTION
    // --------------------------------------------------

    private void showEditDescription(
        ScriptData data,
        BaseDialog oldPreview
    ) {
        BaseDialog edit = new BaseDialog("Edit Script Description");

        TextArea description = new TextArea(data.description);

        edit.cont.add("Edit your description below.")
            .left();

        edit.cont.row();

        edit.cont.add(description)
            .width(500f)
            .height(180f);

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

    // --------------------------------------------------
    // DELETE SCRIPT
    // --------------------------------------------------

    private void confirmDelete(Fi file, BaseDialog preview) {
        BaseDialog confirm = new BaseDialog("Delete Script?");

        confirm.cont.add(
            "Are you sure you want to delete "
                + file.nameWithoutExtension()
                + "?"
        );

        confirm.buttons.button("Delete", () -> {
            file.delete();
            getMetadataFile(file).delete();

            confirm.hide();

            if (preview != null) {
                preview.hide();
            }

            refresh();
        });

        confirm.buttons.button("Cancel", confirm::hide);

        confirm.show();
    }

    // --------------------------------------------------
    // EXPORT SCRIPT
    // --------------------------------------------------

    private void showExport(ScriptData data) {
        BaseDialog export = new BaseDialog("Export Script");

        TextArea packageText = new TextArea(createPackage(data));

        export.cont.add(
            "Copy this exported text to share the script."
        ).left();

        export.cont.row();

        export.cont.add(packageText)
            .width(600f)
            .height(330f);

        export.buttons.button("Close", export::hide);

        export.show();
    }

    private String createPackage(ScriptData data) {
        StringBuilder result = new StringBuilder();

        result.append(PACKAGE_HEADER).append("\n");
        result.append("name=").append(encode(data.name)).append("\n");
        result.append("category=").append(encode(data.category)).append("\n");
        result.append("author=").append(encode(data.author)).append("\n");
        result.append("description=").append(encode(data.description)).append("\n");
        result.append("code=").append(encode(data.code));

        return result.toString();
    }

    // --------------------------------------------------
    // IMPORT SCRIPT
    // --------------------------------------------------

    private void showImport() {
        BaseDialog importDialog = new BaseDialog("Import Script");

        TextField name = new TextField("");
        TextField category = new TextField("My Scripts");
        TextField author = new TextField(getCurrentAuthor());

        TextArea description = new TextArea("");
        TextArea code = new TextArea("");

        importDialog.cont.add("Script Name").left();
        importDialog.cont.row();

        importDialog.cont.add(name)
            .width(480f);

        importDialog.cont.row();

        importDialog.cont.add("Category").left();
        importDialog.cont.row();

        importDialog.cont.add(category)
            .width(480f);

        importDialog.cont.row();

        importDialog.cont.add("Original Author").left();
        importDialog.cont.row();

        importDialog.cont.add(author)
            .width(480f);

        importDialog.cont.row();

        importDialog.cont.add("Description").left();
        importDialog.cont.row();

        importDialog.cont.add(description)
            .width(480f)
            .height(90f);

        importDialog.cont.row();

        importDialog.cont.add(
            "Paste an exported package or raw .mlog code."
        ).left();

        importDialog.cont.row();

        importDialog.cont.add(code)
            .width(600f)
            .height(230f);

        importDialog.buttons.button("Import", () -> {
            String pasted = code.getText();

            if (pasted.trim().isEmpty()) {
                Vars.ui.showInfoToast("Paste a script first.", 2f);
                return;
            }

            ScriptData data;

            try {
                data = parsePackage(pasted);
            } catch (Throwable error) {
                Vars.ui.showInfoToast("Could not read that script package.", 3f);
                return;
            }

            if (data == null) {
                // Plain .mlog code: use the information entered above.
                data = new ScriptData();

                data.name = cleanName(name.getText());
                data.category = cleanName(category.getText());
                data.author = author.getText().trim();
                data.description = description.getText();
                data.code = pasted;

                if (data.name.isEmpty()) {
                    Vars.ui.showInfoToast("Enter a script name.", 2f);
                    return;
                }

                if (data.category.isEmpty()) {
                    data.category = "My Scripts";
                }

                if (data.author.isEmpty()) {
                    data.author = "Anonymous User";
                }
            }

            if (data.name == null || data.name.trim().isEmpty()) {
                Vars.ui.showInfoToast("The script has no name.", 2f);
                return;
            }

            if (data.category == null || data.category.trim().isEmpty()) {
                data.category = "My Scripts";
            }

            if (data.author == null || data.author.trim().isEmpty()) {
                data.author = "Anonymous User";
            }

            data.name = cleanName(data.name);
            data.category = cleanName(data.category);

            saveScript(data);
            importDialog.hide();
        });

        importDialog.buttons.button("Cancel", importDialog::hide);

        importDialog.show();
    }

    private ScriptData parsePackage(String text) {
        String[] lines = text.split("\\r?\\n");

        if (lines.length == 0 ||
            !PACKAGE_HEADER.equals(lines[0].trim())) {
            return null;
        }

        Properties values = new Properties();

        for (int i = 1; i < lines.length; i++) {
            int separator = lines[i].indexOf('=');

            if (separator <= 0) {
                continue;
            }

            String key = lines[i].substring(0, separator);
            String value = lines[i].substring(separator + 1);

            values.setProperty(key, value);
        }

        ScriptData data = new ScriptData();

        data.name = decode(values.getProperty("name", ""));
        data.category = decode(values.getProperty("category", ""));
        data.author = decode(values.getProperty("author", ""));
        data.description = decode(values.getProperty("description", ""));
        data.code = decode(values.getProperty("code", ""));

        return data;
    }

    // --------------------------------------------------
    // TEXT ENCODING FOR MANUAL SHARING
    // --------------------------------------------------

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

    // --------------------------------------------------
    // FILE NAME CLEANUP
    // --------------------------------------------------

    private String cleanName(String name) {
        if (name == null) {
            return "";
        }

        return name.trim()
            .replaceAll("[\\\\/:*?\"<>|]", "_")
            .replaceAll("[. ]+$", "");
    }

    // --------------------------------------------------
    // SCRIPT DATA
    // --------------------------------------------------

    private static class ScriptData {
        String name = "";
        String category = "My Scripts";
        String author = "Anonymous User";
        String description = "";
        String code = "";

        Fi file;
    }

    // --------------------------------------------------
    // OPEN LIBRARY
    // --------------------------------------------------

    @Override
    public void show() {
        refresh();
        super.show();
    }
}
