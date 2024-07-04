package org.github.irengrig.fossil4idea

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Comparing
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/12/13
 * Time: 10:38 AM
 */
class FossilConfigurable(private val myProject: Project) : Configurable {
    private var myPanel: JPanel? = null
    private var myCommandLine: TextFieldWithBrowseButton? = null

    init {
        createUI()
    }

    private fun createUI() {
        myPanel = JPanel(BorderLayout())

        val label = JLabel("Fossil command line client: ")
        val wrapper = JPanel()
        val boxLayout = BoxLayout(wrapper, BoxLayout.X_AXIS)
        wrapper.layout = boxLayout
        wrapper.add(label)
        myCommandLine = TextFieldWithBrowseButton()
        myCommandLine!!.addBrowseFolderListener(
            "Point to Fossil command line", null, myProject,
            FileChooserDescriptor(true, false, false, false, false, false)
        )
        wrapper.add(myCommandLine)
        myPanel!!.add(wrapper, BorderLayout.NORTH)
    }

    @Nls
    override fun getDisplayName(): String {
        return FossilVcs.DISPLAY_NAME
    }

    override fun getHelpTopic(): String? {
        return null
    }

    override fun createComponent(): JComponent? {
        return myPanel
    }

    override fun isModified(): Boolean {
        return !Comparing.equal<String>(
            FossilConfiguration.getInstance(myProject).FOSSIL_PATH,
            myCommandLine!!.text.trim { it <= ' ' })
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        FossilConfiguration.getInstance(myProject).FOSSIL_PATH = myCommandLine!!.text.trim { it <= ' ' }
    }

    override fun reset() {
        myCommandLine!!.text = FossilConfiguration.getInstance(myProject).FOSSIL_PATH
    }

    override fun disposeUIResources() {
    }
}