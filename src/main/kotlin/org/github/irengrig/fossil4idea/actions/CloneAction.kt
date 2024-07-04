package org.github.irengrig.fossil4idea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.ui.VcsBalloonProblemNotifier
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.Consumer
import org.github.irengrig.fossil4idea.checkout.CheckoutUtil
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class CloneAction : AnAction() {
    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = PlatformDataKeys.PROJECT.getData(anActionEvent.dataContext) ?: return

        val uiWorker = UIWorker()
        uiWorker.showDialog(project) {
            ProgressManager.getInstance().run(object :
                Task.Backgroundable(
                    project,
                    "Clone Fossil Repository",
                    false,
                    ALWAYS_BACKGROUND
                ) {
                override fun run(progressIndicator: ProgressIndicator) {
                    try {
                        CheckoutUtil(project).cloneRepo(uiWorker.url, uiWorker.localPath)
                        VcsBalloonProblemNotifier.showOverVersionControlView(
                            project,
                            "Fossil clone successful: " + uiWorker.localPath,
                            MessageType.INFO
                        )
                    } catch (e: VcsException) {
                        VcsBalloonProblemNotifier.showOverVersionControlView(
                            project,
                            "Fossil clone failed: " + e.message,
                            MessageType.ERROR
                        )
                    }
                }
            })
        }
    }

    private class UIWorker {
        private var myLocalPath: TextFieldWithBrowseButton? = null
        private var myUrlField: JTextField? = null

        fun showDialog(project: Project, callback: Runnable) {
            val builder = DialogBuilder(project)
            builder.setCenterPanel(
                createPanel(project,
                    Consumer { aBoolean -> builder.setOkActionEnabled(aBoolean!!) })
            )
            builder.addOkAction()
            builder.addCancelAction()
            builder.setDimensionServiceKey(javaClass.name)
            builder.setTitle("Clone Fossil Repository")
            builder.setOkOperation {
                builder.window.isVisible = false
                callback.run()
            }
            builder.setPreferredFocusComponent(myUrlField)
            builder.show()
        }

        val localPath: String
            get() = myLocalPath!!.text

        val url: String
            get() = myUrlField!!.text

        private fun createPanel(project: Project, enableConsumer: Consumer<Boolean>): JComponent {
            val main = JPanel(GridBagLayout())
            main.minimumSize = Dimension(150, 50)
            val gbc = GridBagConstraints()
            gbc.gridx = 0
            gbc.gridy = 0
            gbc.insets = Insets(2, 2, 2, 2)
            gbc.anchor = GridBagConstraints.NORTHWEST

            main.add(JLabel("Remote URL: "), gbc)
            myUrlField = JTextField(50)
            gbc.gridx++
            gbc.fill = GridBagConstraints.HORIZONTAL
            gbc.weightx = 1.0
            main.add(myUrlField, gbc)
            gbc.gridx = 0
            gbc.gridy++
            gbc.weightx = 0.0
            gbc.fill = GridBagConstraints.NONE
            main.add(JLabel("Local Folder: "), gbc)
            myLocalPath = TextFieldWithBrowseButton()
            myLocalPath!!.addActionListener {
                val dialog = FileChooserFactory.getInstance().createSaveFileDialog(
                    FileSaverDescriptor("Fossil Clone", "Select local file"), project
                )
                val path =
                    FileUtil.toSystemIndependentName(myLocalPath!!.text.trim { it <= ' ' })
                val idx = path.lastIndexOf("/")
                var baseDir = if (idx == -1) project.baseDir else LocalFileSystem.getInstance()
                    .refreshAndFindFileByIoFile(File(path.substring(0, idx)))
                baseDir = baseDir ?: project.baseDir
                val name = if (idx == -1) path else path.substring(idx + 1)
                val fileWrapper = dialog.save(baseDir, name)
                if (fileWrapper != null) {
                    myLocalPath!!.text = fileWrapper.file.path
                }
            }
            /*myLocalPath.addBrowseFolderListener("Select Local File", "Select local file for clone", project,
        new FileSaverDescriptor("Fossil Clone", "Select local file", "checkout", ""));*/
            gbc.weightx = 1.0
            gbc.gridx++
            gbc.fill = GridBagConstraints.HORIZONTAL
            main.add(myLocalPath, gbc)

            /*final ActionListener listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          enableConsumer.consume(!myUrlField.getText().isEmpty() && !myLocalPath.getText().isEmpty());
        }
      };
      myUrlField.addActionListener(listener);
      myLocalPath.addActionListener(listener);*/
            return main
        }
    }
}