package org.github.irengrig.fossil4idea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.ui.VcsBalloonProblemNotifier
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

class OpenAction : AnAction() {
    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = PlatformDataKeys.PROJECT.getData(anActionEvent.dataContext) ?: return

        val uiWorker = CheckoutUIWorker()
        uiWorker.showDialog(project) {
            ProgressManager.getInstance().run(object :
                Task.Backgroundable(
                    project,
                    "Open Fossil Repository",
                    false,
                    ALWAYS_BACKGROUND
                ) {
                override fun run(progressIndicator: ProgressIndicator) {
                    try {
                        CheckoutUtil(project).checkout(
                            File(uiWorker.repo),
                            File(uiWorker.localPath),
                            null
                        )
                        VcsBalloonProblemNotifier.showOverVersionControlView(
                            project,
                            "Fossil repository successfully opened: " + uiWorker.localPath,
                            MessageType.INFO
                        )
                    } catch (e: VcsException) {
                        VcsBalloonProblemNotifier.showOverVersionControlView(
                            project,
                            "Fossil repository not opened: " + e.message,
                            MessageType.ERROR
                        )
                    }
                }
            })
        }
    }

    private class CheckoutUIWorker {
        private var myLocalPath: TextFieldWithBrowseButton? = null
        private var myRepoField: TextFieldWithBrowseButton? = null

        fun showDialog(project: Project?, callback: Runnable) {
            val builder = DialogBuilder(project)
            builder.setCenterPanel(
                createPanel(project,
                    Consumer { aBoolean -> builder.setOkActionEnabled(aBoolean!!) })
            )
            builder.addOkAction()
            builder.addCancelAction()
            builder.setDimensionServiceKey(javaClass.name)
            builder.setTitle("Open Fossil Repository")
            builder.setOkOperation {
                builder.window.isVisible = false
                callback.run()
            }
            builder.setPreferredFocusComponent(myRepoField)
            builder.show()
        }

        val localPath: String
            get() = myLocalPath!!.text

        val repo: String
            get() = myRepoField!!.text

        private fun createPanel(project: Project?, enableConsumer: Consumer<Boolean>): JComponent {
            val main = JPanel(GridBagLayout())
            main.minimumSize = Dimension(150, 50)
            val gbc = GridBagConstraints()
            gbc.gridx = 0
            gbc.gridy = 0
            gbc.insets = Insets(2, 2, 2, 2)
            gbc.anchor = GridBagConstraints.NORTHWEST

            main.add(JLabel("Repository file: "), gbc)
            myRepoField = TextFieldWithBrowseButton()
            myRepoField!!.addBrowseFolderListener(
                "Select Repository",
                null,
                project,
                FileChooserDescriptor(true, false, false, false, false, false)
            )

            gbc.gridx++
            gbc.fill = GridBagConstraints.HORIZONTAL
            gbc.weightx = 1.0
            main.add(myRepoField, gbc)
            gbc.gridx = 0
            gbc.gridy++
            gbc.weightx = 0.0
            gbc.fill = GridBagConstraints.NONE
            main.add(JLabel("Local Folder: "), gbc)
            myLocalPath = TextFieldWithBrowseButton()
            myLocalPath!!.addBrowseFolderListener(
                "Select Folder",
                null,
                project,
                FileChooserDescriptor(false, true, false, false, false, false)
            )
            /*myLocalPath.addBrowseFolderListener("Select Local File", "Select local file for clone", project,
              new FileSaverDescriptor("Fossil Clone", "Select local file", "checkout", ""));*/
            gbc.gridx++
            gbc.weightx = 1.0
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