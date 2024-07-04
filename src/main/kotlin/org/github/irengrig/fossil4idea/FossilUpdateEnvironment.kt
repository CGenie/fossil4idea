package org.github.irengrig.fossil4idea

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.update.*
import org.github.irengrig.fossil4idea.commandLine.FCommandName
import org.github.irengrig.fossil4idea.commandLine.FossilSimpleCommand
import org.github.irengrig.fossil4idea.pull.FossilUpdateConfigurable
import org.github.irengrig.fossil4idea.util.RootUtil.getRemoteUrl
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/15/13
 * Time: 11:55 PM
 */
class FossilUpdateEnvironment(private val myFossilVcs: FossilVcs) : UpdateEnvironment {
    override fun fillGroups(updatedFiles: UpdatedFiles) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Throws(ProcessCanceledException::class)
    override fun updateDirectories(
        contentRoots: Array<FilePath>, updatedFiles: UpdatedFiles,
        progressIndicator: ProgressIndicator, context: Ref<SequentialUpdatesContext>
    ): UpdateSession {
        val exceptions: MutableList<VcsException> = ArrayList()
        val configuration: FossilConfiguration = FossilConfiguration.getInstance(myFossilVcs.project)
        val remoteUrls: Map<File, String> = configuration.remoteUrls

        for (contentRoot in contentRoots) {
            progressIndicator.checkCanceled()
            val remoteUrl = remoteUrls[contentRoot.ioFile]

            try {
                val pull = FossilSimpleCommand(myFossilVcs.project, contentRoot.ioFile, FCommandName.pull)
                if (remoteUrl != null && remoteUrl.length > 0) {
                    pull.addParameters(remoteUrl)
                }
                val pullResult = pull.run()
                /*Round-trips: 2   Artifacts sent: 0  received: 2
Pull finished with 611 bytes sent, 925 bytes received*/
                val update = FossilSimpleCommand(myFossilVcs.project, contentRoot.ioFile, FCommandName.update)
                //        update.addParameters("--debug");
//        update.addParameters("--verbose");
                val out = update.run()
                parseUpdateOut(out, updatedFiles, contentRoot.ioFile)
            } catch (e: VcsException) {
                exceptions.add(e)
            }
        }
        val session = MyUpdateSession(exceptions, progressIndicator.isCanceled)
        return session
    }

    private fun extractMergePath(path: String): String? {
        if (!path.startsWith(MERGE_CONFLICTS1)) return null
        val idx = path.indexOf(MERGE_CONFLICTS2)
        if (idx == -1) return null
        return path.substring(idx + MERGE_CONFLICTS2.length).trim { it <= ' ' }
    }

    private fun parseUpdateOut(out: String?, updatedFiles: UpdatedFiles, ioFile: File) {
        val split = out!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (s in split) {
            val mergePath = extractMergePath(s)
            if (mergePath != null) {
                val file = File(ioFile, mergePath)
                updatedFiles.getGroupById(FileGroup.MERGED_ID).remove(file.path)
                updatedFiles.getGroupById(FileGroup.MERGED_WITH_CONFLICT_ID).add(file.path, FossilVcs.vcsKey, null)
            }
            val idx = s.indexOf(' ')
            if (idx > 0 && idx < (s.length - 1)) {
                val groupId = ourGroupsMapping[s.substring(0, idx)]
                if (groupId != null) {
                    val relative = s.substring(idx).trim { it <= ' ' }
                    val file = File(ioFile, relative)
                    updatedFiles.getGroupById(groupId).add(file.path, FossilVcs.vcsKey, null)
                }
            }
        }
    }

    override fun createConfigurable(files: Collection<FilePath>): Configurable? {
        val checkoutURLs: MutableMap<File, String> =
            HashMap<File, String>(FossilConfiguration.getInstance(myFossilVcs.project).remoteUrls)

        val warnings = StringBuilder()
        ProgressManager.getInstance().runProcessWithProgressSynchronously({
            for (root in files) {
                try {
                    val remoteUrl = getRemoteUrl(myFossilVcs.project, root.ioFile)
                    if (remoteUrl != null) {
                        checkoutURLs[root.ioFile] = remoteUrl
                    }
                } catch (e: VcsException) {
                    warnings.append(e.message).append('\n')
                }
            }
        }, "Getting remote URLs", true, myFossilVcs.project)
        return FossilUpdateConfigurable(myFossilVcs.project, files, checkoutURLs, warnings.toString())
    }

    override fun validateOptions(roots: Collection<FilePath>): Boolean {
        return true
    }

    private class MyUpdateSession(exceptions: List<VcsException>?, isCanceled: Boolean) :
        UpdateSessionAdapter(exceptions, isCanceled)

    companion object {
        private val ourGroupsMapping: MutableMap<String, String> = HashMap()
        private const val MERGE_CONFLICTS2 = "merge conflicts in"
        private const val MERGE_CONFLICTS1 = "*****"

        init {
            ourGroupsMapping["ADD"] = FileGroup.CREATED_ID
            ourGroupsMapping["UPDATE"] = FileGroup.MODIFIED_ID
            ourGroupsMapping["REMOVE"] = FileGroup.REMOVED_FROM_REPOSITORY_ID
            ourGroupsMapping["MERGE"] = FileGroup.MERGED_ID
        }
    }
}