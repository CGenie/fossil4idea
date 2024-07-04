package org.github.irengrig.fossil4idea

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.openapi.vcs.diff.DiffProvider
import com.intellij.openapi.vcs.diff.ItemLatestState
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import org.github.irengrig.fossil4idea.log.CommitWorker
import org.github.irengrig.fossil4idea.repository.FossilContentRevision
import org.github.irengrig.fossil4idea.repository.FossilRevisionNumber
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 3/2/13
 * Time: 7:47 PM
 */
class FossilDiffProvider(private val myFossilVcs: FossilVcs) : DiffProvider {
    override fun getCurrentRevision(file: VirtualFile): VcsRevisionNumber? {
        try {
            return CommitWorker(myFossilVcs.project).getBaseRevisionNumber(File(file.path))
        } catch (e: VcsException) {
            LOG.info(e)
            return null
        }
    }

    override fun getLastRevision(virtualFile: VirtualFile): ItemLatestState? {
        throw UnsupportedOperationException()
    }

    override fun getLastRevision(filePath: FilePath): ItemLatestState? {
        throw UnsupportedOperationException()
    }

    override fun createFileContent(revisionNumber: VcsRevisionNumber, selectedFile: VirtualFile): ContentRevision? {
        return FossilContentRevision(
            myFossilVcs.project,
            LocalFilePath(selectedFile.path, false),
            revisionNumber as FossilRevisionNumber
        )
    }

    override fun getLatestCommittedRevision(vcsRoot: VirtualFile): VcsRevisionNumber? {
        throw UnsupportedOperationException()
    }

    companion object {
        private val LOG = Logger.getInstance("#org.jetbrains.fossil4idea.FossilDiffProvider")
    }
}