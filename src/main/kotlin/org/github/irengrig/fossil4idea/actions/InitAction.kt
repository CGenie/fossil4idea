package org.github.irengrig.fossil4idea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.ui.VcsBalloonProblemNotifier
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWrapper

import org.github.irengrig.fossil4idea.checkout.CheckoutUtil



/**
 * Created by Irina.Chernushina on 5/29/2014.
 */
class InitAction : AnAction() {
    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = PlatformDataKeys.PROJECT.getData(anActionEvent.dataContext) ?: return

        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(
            FileSaverDescriptor(
                "Init Fossil Repository",
                "Select file where to create new Fossil repository."
            ), project
        )
        val wrapper: VirtualFileWrapper = dialog.save(null as VirtualFile?, "$project.fossil") ?: return
        val task: Task.Backgroundable =
            object : Task.Backgroundable(project, "Init Fossil Repository", false, ALWAYS_BACKGROUND) {
                override fun run(progressIndicator: ProgressIndicator) {
                    try {
                        CheckoutUtil(project).initRepository(wrapper.file)
                        VcsBalloonProblemNotifier.showOverVersionControlView(
                            project,
                            "Fossil repository successfully created: " + wrapper.file.path,
                            MessageType.INFO
                        )
                    } catch (e: VcsException) {
                        VcsBalloonProblemNotifier.showOverVersionControlView(
                            project,
                            "Fossil repository not created: " + e.message,
                            MessageType.ERROR
                        )
                    }
                }
            }
        ProgressManager.getInstance().run(task)
    }
}