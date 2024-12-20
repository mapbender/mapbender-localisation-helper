package com.wheregroup.mapbenderlocalisationhelper

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction

/**
 * Expects a hardcoded string in a js file, twig template or php file to be selected.
 * Creates a translation key for the hardcoded strings and adds entries in the translation files.
 */
class ExtractTranslationAction : BaseAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val document = editor.document

        if (!editor.selectionModel.hasSelection()) {
            return showMessage(editor, "Keine Auswahl getroffen", "Wähle den zu extrahierenden Text zunächst aus")
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
        val fileLocation = languages.firstOrNull { it.identifier == "en" }?.location
        if (fileLocation === null) {
            return showMessage(editor, "Outside translation context", "Utility can only be used in a mapbender module with translation files")
        }
        val keys = getKeys(fileLocation)

        var prepopulatedString = ""

        val dialog = ExtractTranslationDialog(project, languages, keys, prepopulatedString, selection) { data ->
            WriteCommandAction.runWriteCommandAction(project) {
                // depending on which file we are in, add code to resolve the translation key
                val replacement = when (file.extension) {
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
                addOrRemoveKey(entry.key.location, data.key, entry.value)
            }
        }
        dialog.show()
    }


}