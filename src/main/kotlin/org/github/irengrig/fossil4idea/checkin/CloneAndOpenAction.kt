package org.github.irengrig.fossil4idea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileChooser.FileChooserDescriptor
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
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.ui.VcsBalloonProblemNotifier
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.Consumer
import org.github.irengrig.fossil4idea.FossilVcs
import org.github.irengrig.fossil4idea.checkout.CheckoutUtil
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import javax.swing.*

class CloneAndOpenAction : AnAction() {
    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = PlatformDataKeys.PROJECT.getData(anActionEvent.dataContext) ?: return

        executeMe(project, null)
    }

    private class UIWorker {
        private var myLocalRepoFile: TextFieldWithBrowseButton? = null
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
            builder.setTitle("Clone and Open Fossil Repository")
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

        val localRepoFile: String
            get() = myLocalRepoFile!!.text

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
            main.add(JLabel("Local Repository File: "), gbc)
            myLocalRepoFile = TextFieldWithBrowseButton()
            myLocalRepoFile!!.addActionListener {
                val dialog = FileChooserFactory.getInstance().createSaveFileDialog(
                    FileSaverDescriptor("Fossil Clone", "Select local file"), project
                )
                val path = FileUtil.toSystemIndependentName(
                    myLocalRepoFile!!.text.trim { it <= ' ' })
                val idx = path.lastIndexOf("/")
                var baseDir = if (idx == -1) project.baseDir else LocalFileSystem.getInstance()
                    .refreshAndFindFileByIoFile(File(path.substring(0, idx)))
                baseDir = baseDir ?: project.baseDir
                val name = if (idx == -1) path else path.substring(idx + 1)
                val fileWrapper = dialog.save(baseDir, name)
                if (fileWrapper != null) {
                    myLocalRepoFile!!.text = fileWrapper.file.path
                }
            }

            gbc.weightx = 1.0
            gbc.gridx++
            gbc.fill = GridBagConstraints.HORIZONTAL
            main.add(myLocalRepoFile, gbc)

            gbc.gridx = 0
            gbc.gridy++
            gbc.fill = GridBagConstraints.NONE
            main.add(JLabel("Local Checkout Folder: "), gbc)
            myLocalPath = TextFieldWithBrowseButton()
            myLocalPath!!.addBrowseFolderListener(
                "Select Checkout Folder",
                null,
                project,
                FileChooserDescriptor(false, true, false, false, false, false)
            )
            /*myLocalPath.addBrowseFolderListener("Select Local File", "Select local file for clone", project,
              new FileSaverDescriptor("Fossil Clone", "Select local file", "checkout", ""));*/
            gbc.weightx = 1.0
            gbc.gridx++
            gbc.fill = GridBagConstraints.HORIZONTAL
            main.add(myLocalPath, gbc)
            return main
        }
    }

    companion object {
        fun executeMe(project: Project, listener: CheckoutProvider.Listener?) {
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
                            progressIndicator.text = "Cloning Fossil Repository..."
                            val localRepoFile = uiWorker.localRepoFile
                            CheckoutUtil(project).cloneRepo(uiWorker.url, localRepoFile)
                            VcsBalloonProblemNotifier.showOverVersionControlView(
                                project,
                                "Fossil clone successful: $localRepoFile", MessageType.INFO
                            )
                            progressIndicator.checkCanceled()
                            progressIndicator.text = "Opening Fossil Repository..."
                            val checkoutPath = uiWorker.localPath
                            val target = File(checkoutPath)
                            CheckoutUtil(project).checkout(File(localRepoFile), target, null)
                            VcsBalloonProblemNotifier.showOverVersionControlView(
                                project,
                                "Fossil repository successfully opened: $checkoutPath",
                                MessageType.INFO
                            )
                            notifyListenerIfNeeded(target, listener)
                        } catch (e: VcsException) {
                            VcsBalloonProblemNotifier.showOverVersionControlView(
                                project,
                                "Fossil clone and open failed: " + e.message,
                                MessageType.ERROR
                            )
                        }
                    }
                })
            }
        }

        private fun notifyListenerIfNeeded(target: File, listener: CheckoutProvider.Listener?) {
            if (listener != null) {
                val lfs = LocalFileSystem.getInstance()
                val vf = lfs.refreshAndFindFileByIoFile(target)
                if (vf != null) {
                    vf.refresh(true, true,
                        Runnable { SwingUtilities.invokeLater { notifyListener(listener, target) } })
                } else {
                    notifyListener(listener, target)
                }
            }
        }

        private fun notifyListener(listener: CheckoutProvider.Listener, target: File) {
            listener.directoryCheckedOut(target, FossilVcs.vcsKey)
            listener.checkoutCompleted()
        }
    }
}