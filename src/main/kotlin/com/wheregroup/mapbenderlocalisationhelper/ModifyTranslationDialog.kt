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


class ModifyTranslationDialog(
    project: Project,
    key: String,
    val languages: List<Pair<Language, String?>>,
    val onClickAction: OnClick
) : DialogWrapper(project, true) {

    val tfKey = JTextField(key)
    val tfLanguages = hashMapOf<Language, JTextField>()
    val warningBox = HorizontalBox()

    init {
        title = "Übersetzung bearbeiten"

        languages.forEach {entry ->
            val textField = JTextField()
            textField.text = entry.second
            tfLanguages[entry.first] = textField
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

        label.preferredSize = Dimension(100, 20)
        label.labelFor = tfKey
        panel.add(label)
        panel.add(tfKey)
        panel.add(warningBox)
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



}