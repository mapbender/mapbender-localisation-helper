package com.wheregroup.mapbenderlocalisationhelper

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction

/**
 * Expects a hardcoded string in a js file, twig template or php file to be selected.
 * Creates a translation key for the hardcoded strings and adds entries in the translation files.
 */
class ModifyTranslationAction : BaseAction() {
    // yaml formatting options
    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val document = editor.document

        if (!editor.selectionModel.hasSelection()) {
            return showMessage(editor, "Keine Auswahl getroffen", "Wähle den translation-String zunächst aus")
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
            return showMessage(
                editor,
                "Outside translation context",
                "Utility can only be used in a mapbender module with translation files"
            )
        }

        val mappings = languages.map { language ->
            println(language.identifier + ": " + language.location)
            Pair(language, findValue(language.location, selection))
        }

        val dialog = ModifyTranslationDialog(project, selection, mappings) { data ->
            data.translations.forEach { entry ->
                if (selection == data.key) {
                    addOrRemoveKey(entry.key.location, data.key, entry.value)
                } else {
                    addOrRemoveKeys(entry.key.location, mapOf(
                        Pair(data.key, entry.value),
                        Pair(selection, null),
                    ))
                }
            }

            if (selection != data.key) {
                WriteCommandAction.runWriteCommandAction(project) {
                    val replacement = quoteChar + data.key + quoteChar
                    document.replaceString(start, end, replacement)
                }
            }
        }
        dialog.show()

    }
}