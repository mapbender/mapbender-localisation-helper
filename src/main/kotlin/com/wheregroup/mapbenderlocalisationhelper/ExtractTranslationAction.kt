package com.wheregroup.mapbenderlocalisationhelper

import com.intellij.codeInsight.hint.HintUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDirectory
import com.intellij.util.ui.JBUI
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileWriter

/**
 * Expects a hardcoded string in a js file, twig template or php file to be selected.
 * Creates a translation key for the hardcoded strings and adds entries in the translation files.
 */
class ExtractTranslationAction : AnAction() {
    // yaml formatting options
    private val dumperOptions = DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        isPrettyFlow = true
        splitLines = false
        width = 100000
    }
    private val yamlParser = Yaml(ComplexDataExpressionRepresenter(dumperOptions), dumperOptions)

    override fun update(event: AnActionEvent) {
        // Using the event, evaluate the context,
        // and enable or disable the action.
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val document = editor.document

        if (!editor.selectionModel.hasSelection()) {
            return showNothingSelectedMessage(editor)
        }

        val primaryCaret = editor.caretModel.primaryCaret
        val start = primaryCaret.selectionStart
        val end = primaryCaret.selectionEnd
        var selection = primaryCaret.selectedText.toString()

        // ignore quotes in the selection
        var quoteChar = ""
        if (selection.startsWith("'") && selection.endsWith("'")) quoteChar = "'"
        if (selection.startsWith("\"") && selection.endsWith("\"")) quoteChar = "\""
        if (quoteChar != "") selection = selection.substring(1, selection.length - 1)

        val languages = getLanguages(file)
        val keys = getKeys(languages.first { it.identifier == "en" }.location)

        var prepopulatedString = ""

        val dialog = ExtractTranslationDialog(project, languages, keys, prepopulatedString, selection) { data ->
            WriteCommandAction.runWriteCommandAction(project) {
                // depending on which file we are in, add code to resolve the translation key
                val replacement = when(file.extension) {
                    "js" -> "Mapbender.trans('" + data.key + "')"
                    "twig", "html.twig", "html" -> "{{ '" + data.key + "' | trans }}"
                    else -> quoteChar + data.key + quoteChar
                }

                // replace the selected text by the entered key
                document.replaceString(start, end, replacement)
                val split = data.key.split(".")
                prepopulatedString = split.subList(0, split.size - 1).joinToString(".")
                if (prepopulatedString.isNotBlank()) prepopulatedString += "."
            }

            data.translations.forEach { entry ->
                addKey(entry.key.location, data.key, entry.value)
            }
        }
        dialog.show()
    }

    // adds a generated key to a language file
    private fun addKey(location: String, key: String, value: String) {
        if (value.isBlank()) return
        if (key.isBlank()) return

        val file = LocalFileSystem.getInstance().findFileByIoFile(File(location)) ?: return

        val parsed: LinkedHashMap<String, Any> = yamlParser.load(file.inputStream)
        val keyComponents = ArrayList(key.split("."))

        var searchModel = parsed
        while (keyComponents.size > 1) {
            val first = keyComponents[0]
            keyComponents.removeAt(0)
            if (searchModel.containsKey(first)) {
                searchModel = (searchModel[first] as? LinkedHashMap<String, Any>) ?: continue
            } else {
                val newMap = LinkedHashMap<String, Any>()
                searchModel[first] = newMap
                searchModel = newMap
            }
        }
        searchModel[keyComponents[0]] = value
        yamlParser.dump(parsed, FileWriter(File(location)))
    }

    /**
     * moves up to the closest Bundle file or the application directory and detects all language files present there
     */
    private fun getLanguages(file: VirtualFile): List<Language> {
        var _file = file
        while (!_file.isDirectory || !(_file.name.contains("Bundle") || _file.name == "application"))
            _file = _file.parent

        val translations = _file.findDirectory(if (_file.name == "application") "translations" else "Resources/translations")?.children
        return translations?.mapNotNull {
            if (!it.nameWithoutExtension.startsWith("messages")) return@mapNotNull null;
            Language(it.nameWithoutExtension.replace("messages.", "").replace("messages+intl-icu.", ""), it.path)
        } ?: listOf()
    }

    // returns all languages keys present in a language file
    private fun getKeys(fileLocation: String): List<String> {
        val file = LocalFileSystem.getInstance().findFileByIoFile(File(fileLocation)) ?: return listOf()
        val parsed: LinkedHashMap<String, Any> = yamlParser.load(file.inputStream)
        val keys = ArrayList<String>()
        addKeysToList("", keys, parsed)
        return keys
    }

    private fun addKeysToList(namespace: String, outList: java.util.ArrayList<String>, inMap: java.util.LinkedHashMap<String, Any>) {
        outList.addAll(inMap.keys.map { namespace + it })
        inMap.entries.forEach {
            if (it.value is Map<*, *>) {
                addKeysToList(namespace + it.key + ".", outList, it.value as java.util.LinkedHashMap<String, Any>)
            }
        }
    }

    private fun showNothingSelectedMessage(editor: Editor) {
        val label = HintUtil.createInformationLabel("Wähle den zu extrahierenden Text zunächst aus");
        label.border = JBUI.Borders.empty(2, 7);
        JBPopupFactory.getInstance().createDialogBalloonBuilder(label, "Keine Auswahl getroffen").setFadeoutTime(3000)
            .setFillColor(HintUtil.getInformationColor()).createBalloon()
            .show(JBPopupFactory.getInstance().guessBestPopupLocation(editor), Balloon.Position.below)
    }
}