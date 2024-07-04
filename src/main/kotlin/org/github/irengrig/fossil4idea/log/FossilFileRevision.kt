package org.github.irengrig.fossil4idea.log

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.RepositoryLocation
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import org.github.irengrig.fossil4idea.repository.FossilContentRevision
import org.github.irengrig.fossil4idea.repository.FossilRevisionNumber
import java.io.IOException
import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/24/13
 * Time: 8:13 PM
 */
class FossilFileRevision(
    project: Project?, filePath: FilePath?, revisionNumber: FossilRevisionNumber?,
    private val myAuthor: String, private val myComment: String
) : VcsFileRevision {
    private val myContentRevision = FossilContentRevision(project!!, filePath!!, revisionNumber!!)

    override fun getBranchName(): String? {
        return null
    }

    override fun getChangedRepositoryPath(): RepositoryLocation? {
        return FossilRepositoryLocation(myContentRevision.file.ioFile)
    }

    @Throws(IOException::class, VcsException::class)
    override fun loadContent(): ByteArray {
        return content!!
    }

    @Throws(IOException::class, VcsException::class)
    override fun getContent(): ByteArray? {
        val content = myContentRevision.content
        return content?.toByteArray()
    }

    override fun getRevisionNumber(): VcsRevisionNumber {
        return myContentRevision.revisionNumber
    }

    override fun getRevisionDate(): Date {
        return myContentRevision.revisionNumber.date!!
    }

    override fun getAuthor(): String? {
        return myAuthor
    }

    override fun getCommitMessage(): String? {
        return myComment
    }
}