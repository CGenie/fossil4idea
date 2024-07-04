package org.github.irengrig.fossil4idea.log

import com.intellij.openapi.vcs.RepositoryLocation
import com.intellij.openapi.vcs.VcsException
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/24/13
 * Time: 8:14 PM
 */
class FossilRepositoryLocation(private val myFile: File) : RepositoryLocation {
    override fun toPresentableString(): String {
        return myFile.path
    }

    override fun getKey(): String {
        return myFile.path
    }

    @Throws(VcsException::class)
    override fun onBeforeBatch() {
    }

    override fun onAfterBatch() {
    }
}