package org.github.irengrig.fossil4idea.repository

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Throwable2Computable
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.openapi.vcs.impl.ContentRevisionCache
import org.github.irengrig.fossil4idea.FossilException
import org.github.irengrig.fossil4idea.FossilVcs
import org.github.irengrig.fossil4idea.log.CatWorker
import java.io.IOException

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/13/13
 * Time: 11:02 PM
 */
class FossilContentRevision(val project: Project, private val myFilePath: FilePath, number: FossilRevisionNumber) :
    ContentRevision {
    private val myNumber: FossilRevisionNumber = number

    @Throws(VcsException::class)
    override fun getContent(): String? {
        try {
            return ContentRevisionCache.getOrLoadAsString(
                project,
                myFilePath,
                myNumber,
                FossilVcs.vcsKey,
                ContentRevisionCache.UniqueType.REPOSITORY_CONTENT,
                object : Throwable2Computable<ByteArray, VcsException?, IOException?> {
                    @Throws(VcsException::class, IOException::class)
                    override fun compute(): ByteArray {
                        return CatWorker(project).cat(myFilePath.ioFile, myNumber.hash)!!.toByteArray()
                    }
                })
        } catch (e: IOException) {
            throw FossilException(e)
        }
    }

    override fun getFile(): FilePath {
        return myFilePath
    }

    override fun getRevisionNumber(): FossilRevisionNumber {
        return myNumber
    }
}