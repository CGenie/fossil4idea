package org.github.irengrig.fossil4idea.pull

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MultiLineLabelUI
import com.intellij.openapi.vcs.FilePath
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import org.github.irengrig.fossil4idea.FossilConfiguration
import org.github.irengrig.fossil4idea.util.TextUtil
import org.jetbrains.annotations.Nls
import java.awt.*
import java.io.File
import javax.swing.JComponent
import javax.swing.JLabel

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 3/2/13
 * Time: 8:35 PM
 */
class FossilUpdateConfigurable(
    private val myProject: Project,
    private val myRoots: Collection<FilePath>,
    private val myCheckoutURLs: Map<File, String>,
    private val myWarning: String?
) :
    Configurable {
    private val myFields: MutableMap<File, JBTextField> = HashMap()

    @Nls
    override fun getDisplayName(): String {
        return "Fossil Update Settings"
    }

    override fun getHelpTopic(): String? {
        return null
    }

    override fun createComponent(): JComponent? {
        val panel: JBPanel<*> = JBPanel<JBPanel<*>>(GridBagLayout())
        if (myRoots.size > 1) {
            buildForMultiple(panel)
        } else {
            buildForOne(panel, myRoots.iterator().next())
        }
        return panel
    }

    private fun buildForOne(panel: JBPanel<*>, root: FilePath) {
        val c = GridBagConstraints(
            0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
            GridBagConstraints.NONE, Insets(1, 1, 1, 1), 0, 0
        )
        c.fill = GridBagConstraints.HORIZONTAL
        c.gridwidth = 2
        val comp = JBLabel("Please select remote URL:")
        comp.font = comp.font.deriveFont(Font.BOLD)
        panel.add(comp, c)
        val value = JBTextField()
        value.columns = 100
        val preset = myCheckoutURLs[root.ioFile]
        if (preset != null) {
            value.text = preset
        }
        myFields[root.ioFile] = value
        ++c.gridy
        panel.add(value, c)
        addWarning(panel, c)
    }

    private fun buildForMultiple(panel: JBPanel<*>) {
        val c = GridBagConstraints(
            0,
            0,
            1,
            1,
            1.0,
            1.0,
            GridBagConstraints.NORTHWEST,
            GridBagConstraints.NONE,
            Insets(1, 1, 1, 1),
            0,
            0
        )
        c.gridwidth = 2
        c.fill = GridBagConstraints.HORIZONTAL
        val comp = JBLabel("Please select remote URLs for roots:")
        comp.font = comp.font.deriveFont(Font.BOLD)
        panel.add(comp, c)
        c.gridwidth = 1

        for (root in myRoots) {
            c.weighty = 0.0
            c.gridx = 0
            ++c.gridy
            c.fill = GridBagConstraints.NONE
            panel.add(JBLabel(root.name + " (" + root.parentPath + ")"), c)
            ++c.gridx
            c.fill = GridBagConstraints.HORIZONTAL
            c.weighty = 1.0
            val field = JBTextField()
            panel.add(field, c)
            myFields[root.ioFile] = field
            val preset = myCheckoutURLs[root.ioFile]
            if (preset != null) {
                field.text = preset
            }
        }
        addWarning(panel, c)
    }

    private fun addWarning(panel: JBPanel<*>, c: GridBagConstraints) {
        if (myWarning != null && myWarning.length > 0) {
            ++c.gridy
            c.gridx = 0
            c.gridwidth = 2
            c.fill = GridBagConstraints.HORIZONTAL
            val label = JLabel(TextUtil().insertLineCuts("Warning: $myWarning"))
            label.setUI(MultiLineLabelUI())
            label.foreground = SimpleTextAttributes.ERROR_ATTRIBUTES.fgColor
            panel.add(label, c)
        }
    }

    override fun isModified(): Boolean {
        return false
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val configuration = FossilConfiguration.getInstance(myProject)

        val urls: MutableMap<File, String> = HashMap(configuration.remoteUrls)
        for ((key, value) in myFields) {
            val text = value.text
            if (text.trim { it <= ' ' } == myCheckoutURLs[key] || text.trim { it <= ' ' }.length == 0) {
                // remove override from map
                urls.remove(key)
            } else {
                urls[key] = text.trim { it <= ' ' }
            }
        }
        configuration.remoteUrls = urls
    }

    override fun reset() {
    }

    override fun disposeUIResources() {
    }
}