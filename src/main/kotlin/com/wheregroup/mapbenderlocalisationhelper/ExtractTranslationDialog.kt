package com.wheregroup.mapbenderlocalisationhelper

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.panels.HorizontalBox
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBImageIcon
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.JTextField

typealias OnClick = (data: ExtractTranslationResultData) -> Unit

class Language(
    val identifier: String,
    val location: String,
)

class ExtractTranslationDialog(
    project: Project,
    val languages: List<Language>,
    val availableKeys: List<String>,
    val prepopulatedString: String,
    selection: String,
    val onClickAction: OnClick
) : DialogWrapper(project, true) {

    val tfKey = JTextField(prepopulatedString)
    val tfLanguages = hashMapOf<Language, JTextField>()
    val warningBox = HorizontalBox()

    init {
        title = "String als Translation extrahieren"

        languages.forEach {entry ->
            val textField = JTextField()
            if (entry.identifier == "en") {
                textField.text = selection
            }
            tfLanguages[entry] = textField
        }

        init()

        getButton(okAction)?.addActionListener {
            onClickAction.invoke(
                ExtractTranslationResultData(
                    tfKey.text,
                    tfLanguages.mapValues { entry -> entry.value.text })
            )
        }
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return tfKey
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(VerticalLayout(10))
        val label = JLabel("Übersetzungskey")

        warningBox.add(JLabel("ACHTUNG: dieser Key existiert bereits und wird überschrieben"))
        warningBox.isVisible = false

        label.preferredSize = Dimension(100, 20)
        label.labelFor = tfKey
        panel.add(label)
        panel.add(tfKey)
        panel.add(warningBox)
        panel.add(setupAutocomplete())
        panel.add(JSeparator())

        tfLanguages.entries.sortedBy { it.key.identifier }.forEach { entry ->
            val layout = HorizontalBox()
            val jLabel = JLabel(entry.key.identifier)
            jLabel.labelFor = entry.value
            jLabel.preferredSize = Dimension(50, 20)
            layout.add(jLabel)
            layout.add(entry.value)
            panel.add(layout)
        }

        return panel
    }

    private fun setupAutocomplete(): JComponent {

        var filteredKeys = availableKeys.filter { it.startsWith(prepopulatedString) }.toTypedArray()
        val list = JList(filteredKeys)

        val scrollContainer = JScrollPane()
        scrollContainer.setViewportView(list)
        scrollContainer.preferredSize = Dimension(200, 120)

        AutoCompleteDecorator.decorate(tfKey, availableKeys, false, ObjectToStringConverter.DEFAULT_IMPLEMENTATION)

        tfKey.addKeyListener(object : KeyListener {
            override fun keyTyped(p0: KeyEvent?) {
            }

            override fun keyPressed(p0: KeyEvent?) {
            }

            override fun keyReleased(p0: KeyEvent?) {
                filteredKeys = availableKeys.filter { it.startsWith(tfKey.text) }.toTypedArray()
                list.setListData(filteredKeys)

                warningBox.isVisible = availableKeys.contains(tfKey.text)
            }

        })

        list.addListSelectionListener {
            if (it.firstIndex >= 0 && it.firstIndex < filteredKeys.size) {
                tfKey.text = filteredKeys[it.firstIndex]
                list.selectedIndices = intArrayOf()
            }
        }

        return scrollContainer
    }


}