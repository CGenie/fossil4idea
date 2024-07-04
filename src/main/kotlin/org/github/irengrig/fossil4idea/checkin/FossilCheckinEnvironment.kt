package org.github.irengrig.fossil4idea.checkin

import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeList
import com.intellij.openapi.vcs.changes.ChangesUtil
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinEnvironment
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.FunctionUtil
import com.intellij.util.NullableFunction
import com.intellij.util.PairConsumer
import com.intellij.util.containers.ContainerUtil
import org.github.irengrig.fossil4idea.FossilVcs
import org.github.irengrig.fossil4idea.util.FossilUtils
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/14/13
 * Time: 6:52 PM
 */
class FossilCheckinEnvironment(private val myFossilVcs: FossilVcs) : CheckinEnvironment {
    // don't like it, but because of platform design
    private var myPush = false

    @Deprecated("Deprecated in Java")
    override fun createAdditionalOptionsPanel(
        panel: CheckinProjectPanel,
        additionalDataConsumer: PairConsumer<Any, Any>
    ): RefreshableOnComponent? {
        return null
    }

    override fun getDefaultMessageFor(filesToCheckin: Array<FilePath>): String? {
        return null
    }

    override fun getHelpId(): String? {
        return null
    }

    override fun getCheckinOperationName(): String {
        return "Commit"
    }

    //@JvmOverloads
    override fun commit(
        changes: MutableList<out Change?>,
        preparedComment: String,
        commitContext: CommitContext,
        feedback: MutableSet<in String?>
    ): List<VcsException>? {
        val wasToPush = myPush
        myPush = false
        val checkinUtil: CheckinUtil = CheckinUtil(myFossilVcs.project)
        val files = ChangesUtil.getIoFilesFromChanges(changes!!)
        val exceptions: MutableList<VcsException> = ArrayList()
        try {
            val hashes: List<String> = checkinUtil.checkin(files, preparedComment)
            if (hashes != null && !hashes.isEmpty()) {
                feedback.add(createMessage(hashes))
                    ?: // popup
                    PopupUtil.showBalloonForActiveComponent(createMessage(hashes), MessageType.INFO)
                // something committed & need to push
                if (wasToPush) {
                    checkinUtil.push()
                }
            }
        } catch (e: VcsException) {
            exceptions.add(e)
        }
        return exceptions
    }

    private fun createMessage(hashes: List<String>): String {
        return "Fossil: committed: " + StringUtil.join(hashes, ",")
    }

    override fun scheduleMissingFileForDeletion(files: List<FilePath>): List<VcsException>? {
        val result: MutableList<VcsException> = ArrayList()
        try {
            AddUtil.deleteImpl(
                myFossilVcs.project,
                ContainerUtil.map(files, FossilUtils.FILE_PATH_FILE_CONVERTOR)
            )
        } catch (e: VcsException) {
            result.add(e)
        }
        return result
    }

    override fun scheduleUnversionedFilesForAddition(files: List<VirtualFile>): List<VcsException>? {
        val result: MutableList<VcsException> = ArrayList()
        try {
            AddUtil.scheduleForAddition(
                myFossilVcs.project, ContainerUtil.map(files
                ) { o -> File(o.path) }
            )
        } catch (e: VcsException) {
            result.add(e)
        }
        return result
    }

    override fun keepChangeListAfterCommit(changeList: ChangeList): Boolean {
        return false
    }

    override fun isRefreshAfterCommitNeeded(): Boolean {
        return false
    }

    fun setPush() {
        myPush = true
    }
}