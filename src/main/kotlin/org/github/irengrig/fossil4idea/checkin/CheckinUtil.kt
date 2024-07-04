package org.github.irengrig.fossil4idea.checkin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsException
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.UIUtil
import org.github.irengrig.fossil4idea.FossilConfiguration
import org.github.irengrig.fossil4idea.FossilException
import org.github.irengrig.fossil4idea.FossilVcs
import org.github.irengrig.fossil4idea.checkin.AddUtil.tryFindCommonParent
import org.github.irengrig.fossil4idea.commandLine.FCommandName
import org.github.irengrig.fossil4idea.commandLine.FossilSimpleCommand
import org.github.irengrig.fossil4idea.local.MoveWorker
import org.github.irengrig.fossil4idea.pull.FossilUpdateConfigurable
import java.io.File
import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/24/13
 * Time: 2:59 PM
 */
class CheckinUtil(private val myProject: Project) {
    fun push() {
        val fossil = FossilVcs.getInstance(myProject)
        val roots = ProjectLevelVcsManager.getInstance(myProject).getRootsUnderVcs(
            fossil!!
        )
        if (roots.size == 0) {
//      PopupUtil.showBalloonForActiveComponent("Error occurred while pushing: No roots under Fossil found.", MessageType.ERROR);
            return
        }

        UIUtil.invokeAndWaitIfNeeded(object : Runnable {
            override fun run() {
                val filePaths = ContainerUtil.map(roots){ o -> LocalFilePath(o.path, false) }
                val configurable = fossil.updateEnvironment!!.createConfigurable(filePaths as Collection<FilePath>?) as FossilUpdateConfigurable?
                val component = configurable!!.createComponent()
                val builder = DialogBuilder(myProject)
                builder.setCenterPanel(component)
                builder.addOkAction()
                builder.addCancelAction()
                builder.setDimensionServiceKey(javaClass.name)
                builder.setTitle("Push into Fossil Repository")
                builder.setOkOperation {
                    builder.window.isVisible = false
                    try {
                        configurable.apply()
                        val pi = ProgressManager.getInstance().progressIndicator
                        if (pi != null) {
                            pi.text = "Pushing..."
                        }
                        ApplicationManager.getApplication().executeOnPooledThread {
                            try {
                                val s = pushImpl()
                                PopupUtil.showBalloonForActiveComponent(s, MessageType.INFO)
                            } catch (e: VcsException) {
                                // todo as notification
                                PopupUtil.showBalloonForActiveComponent(
                                    "Error occurred while pushing: " + e.message,
                                    MessageType.ERROR
                                )
                            }
                        }
                    } catch (e: ConfigurationException) {
                        PopupUtil.showBalloonForActiveComponent(
                            "Error occurred while pushing: " + e.message,
                            MessageType.ERROR
                        )
                    }
                }
                //        builder.setPreferredFocusComponent(configurable.);
                builder.show()
            }
        })
    }

    @Throws(VcsException::class)
    private fun pushImpl(): String {
        val sb = StringBuilder()
        val instance = FossilConfiguration.getInstance(myProject)
        val remoteUrls = instance.remoteUrls

        val fossil = FossilVcs.getInstance(myProject)
        val roots = ProjectLevelVcsManager.getInstance(myProject).getRootsUnderVcs(
            fossil!!
        )
        if (roots.size == 0) throw FossilException("No roots under Fossil found.")

        for (root in roots) {
            val file = File(root.path)
            val remote = remoteUrls[file]
            var s = pushOneRoot(file, remote)
            s = if (s.isEmpty() && remote != null) "Pushed to $remote" else s
            if (!s.isEmpty()) {
                if (sb.length > 0) sb.append("\n")
                sb.append(s)
            }
        }
        return sb.toString()
    }

    @Throws(VcsException::class)
    private fun pushOneRoot(file: File, url: String?): String {
        val command = FossilSimpleCommand(myProject, file, FCommandName.push, BREAK_SEQUENCE)
        if (url != null) {
            command.addParameters(url)
        }
        val run = command.run()
        val split = run!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (s in split) {
            if (s.startsWith("Error: ")) {
                throw FossilException(s)
            }
            if (s.startsWith(PUSH_TO)) {
                return s.substring(PUSH_TO.length)
            }
        }
        /*Push to file://D:/testprojects/_fc_/r/repo_1*/
        return ""
    }

    /**
     * @return list of committed revisions hashes
     */
    @Throws(VcsException::class)
    fun checkin(files: List<File>, comment: String?): List<String> {
        val parent = tryFindCommonParent(myProject, files)
        if (parent != null) {
            var result: String? = null
            for (i in 0..1) {
                val command = FossilSimpleCommand(myProject, parent, FCommandName.commit, BREAK_SEQUENCE)
                command.addParameters("--no-warnings")

                // maybe helps someday
                command.addAnswerYes("Commit anyhow (a=all/c=convert/y/N)?")
                command.addAnswerYes("continue in spite of time skew (y/N)?")

                //continue in spite of sync failure (y/N)?
                command.addBreakSequence("fossil knows nothing about")
                command.addBreakSequence(QUESTION)
                command.addBreakSequence("Autosync failed")
                command.addSkipError("Abandoning commit due to CR/NL line endings")
                command.addParameters("-m", StringUtil.escapeStringCharacters(comment!!))
                for (file in files) {
                    val relative = FileUtil.getRelativePath(parent, file)
                    command.addParameters(relative!!)
                }
                result = command.run()
                if (result!!.contains(QUESTION) && result.contains(BREAK_SEQUENCE)) {
                    val ok = IntArray(1)
                    UIUtil.invokeAndWaitIfNeeded {
                        ok[0] = Messages.showOkCancelDialog(
                            myProject,
                            """
                                File(s) you are attempting to commit, contain CR/NL line endings;
                                Fossil plugin needs to disable CR/NL check by changing crnl-glob setting to '*'.
                                Do you wish to change crnl-glob setting and continue?
                                """.trimIndent(),
                            "CR/NL line endings",
                            Messages.getQuestionIcon()
                        )
                    }
                    if (ok[0] == Messages.OK) {
                        val settingsCommand = FossilSimpleCommand(myProject, parent, FCommandName.settings)
                        settingsCommand.addParameters("crnl-glob", "'*'")
                        settingsCommand.run()
                        continue
                    }
                }
                break
            }
            return listOf(parseHash(result))
        } else {
            val hashes: MutableList<String> = ArrayList()
            for (file in files) {
                val command = FossilSimpleCommand(myProject, MoveWorker.findParent(file), FCommandName.commit)
                command.addParameters(
                    "-m", "\"" + StringUtil.escapeStringCharacters(
                        comment!!
                    ) + "\""
                )
                command.addParameters(file.path)
                hashes.add(parseHash(command.run()))
            }
            return hashes
        }
    }

    @Throws(FossilException::class)
    private fun parseHash(result: String?): String {
        var result1 = result ?: throw FossilException("Can not parse 'commit' result: null")
        result1 = result1.trim { it <= ' ' }
        val split = result1.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in split.indices) {
            val s = split[i]
            if (s.startsWith(PREFIX)) return s.substring(PREFIX.length).trim { it <= ' ' }
        }
        throw FossilException("Can not parse 'commit' result: $result1")
    }

    companion object {
        const val QUESTION: String = "Commit anyhow (a=all/c=convert/y/N)?"
        const val BREAK_SEQUENCE: String = "contains CR/NL line endings"
        const val PUSH_TO: String = "Push to"
        const val PREFIX: String = "New_Version: "
    }
}