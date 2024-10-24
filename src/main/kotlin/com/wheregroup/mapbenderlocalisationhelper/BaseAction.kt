package com.wheregroup.mapbenderlocalisationhelper

import com.intellij.codeInsight.hint.HintUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
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

abstract class BaseAction : AnAction() {

    // yaml formatting options
    protected val dumperOptions = DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        isPrettyFlow = true
        splitLines = false
        width = 100000
    }
    protected val yamlParser = Yaml(ComplexDataExpressionRepresenter(dumperOptions), dumperOptions)

    override fun update(event: AnActionEvent) {
        // Using the event, evaluate the context,
        // and enable or disable the action.
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    protected fun showMessage(
        editor: Editor,
        title: String,
        description: String
    ) {
        val label = HintUtil.createInformationLabel(description);
        label.border = JBUI.Borders.empty(2, 7);
        JBPopupFactory.getInstance().createDialogBalloonBuilder(label, title).setFadeoutTime(3000)
            .setFillColor(HintUtil.getInformationColor()).createBalloon()
            .show(JBPopupFactory.getInstance().guessBestPopupLocation(editor), Balloon.Position.below)
    }

    protected fun addOrRemoveKey(location: String, key: String, value: String?) {
        addOrRemoveKeys(location, mapOf(Pair(key, value)))
    }

    // adds a generated key to a language file
    protected fun addOrRemoveKeys(location: String, values: Map<String, String?>) {

        val file = LocalFileSystem.getInstance().findFileByIoFile(File(location)) ?: return
        val parsed: LinkedHashMap<String, Any> = yamlParser.load(file.inputStream)

        values.entries.forEach { (key, value) ->
            if (key.isBlank()) return@forEach
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
            if (value.isNullOrBlank()) {
                if (searchModel.containsKey(keyComponents[0])) searchModel.remove(keyComponents[0])
            } else {
                searchModel[keyComponents[0]] = value
            }
        }

        yamlParser.dump(parsed, FileWriter(File(location)))
    }

    /**
     * moves up to the closest Bundle file or the application directory and detects all language files present there
     */
    protected fun getLanguages(file: VirtualFile): List<Language> {
        var _file = file
        while (!_file.isDirectory || !(_file.name.contains("Bundle") || _file.name == "application"))
            _file = _file.parent

        val translations =
            _file.findDirectory(if (_file.name == "application") "translations" else "Resources/translations")?.children
        return translations?.mapNotNull {
            if (!it.nameWithoutExtension.startsWith("messages")) return@mapNotNull null;
            Language(it.nameWithoutExtension.replace("messages.", "").replace("messages+intl-icu.", ""), it.path)
        } ?: listOf()
    }

    protected fun findValue(fileLocation: String, key: String): String? {
        val file = LocalFileSystem.getInstance().findFileByIoFile(File(fileLocation)) ?: return null
        val parsed: LinkedHashMap<String, Any> = yamlParser.load(file.inputStream)
        return _findValue(parsed, key.split(".").toMutableList())
    }

    private fun _findValue(parsed: java.util.LinkedHashMap<String, Any>, lookup: MutableList<String>): String? {
        val nextKey = lookup.removeFirst()
        if (!parsed.containsKey(nextKey)) return null
        val nextValue = parsed.get(nextKey)

        if (lookup.isEmpty()) {
            return (if (nextValue is String) nextValue else null)
        }
        return _findValue(nextValue as java.util.LinkedHashMap<String, Any>, lookup)
    }


    // returns all languages keys present in a language file
    protected fun getKeys(fileLocation: String): List<String> {
        val file = LocalFileSystem.getInstance().findFileByIoFile(File(fileLocation)) ?: return listOf()
        val parsed: LinkedHashMap<String, Any> = yamlParser.load(file.inputStream)
        val keys = ArrayList<String>()
        addKeysToList("", keys, parsed)
        return keys
    }

    protected fun addKeysToList(
        namespace: String,
        outList: java.util.ArrayList<String>,
        inMap: java.util.LinkedHashMap<String, Any>
    ) {
        outList.addAll(inMap.keys.map { namespace + it })
        inMap.entries.forEach {
            if (it.value is Map<*, *>) {
                addKeysToList(namespace + it.key + ".", outList, it.value as java.util.LinkedHashMap<String, Any>)
            }
        }
    }

}