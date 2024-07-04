package org.github.irengrig.fossil4idea.local

import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.rollback.DefaultRollbackEnvironment
import com.intellij.openapi.vcs.rollback.RollbackProgressListener
import org.github.irengrig.fossil4idea.FossilVcs

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/15/13
 * Time: 11:55 PM
 */
class FossilRollbackEnvironment(private val myFossilVcs: FossilVcs) : DefaultRollbackEnvironment() {
    override fun rollbackChanges(
        changes: List<Change?>?, vcsExceptions: MutableList<VcsException?>,
        listener: RollbackProgressListener
    ) {
        try {
            LocalUtil.rollbackChanges(myFossilVcs.project, changes, listener)
        } catch (e: VcsException) {
            vcsExceptions.add(e)
        }
    }

    override fun rollbackMissingFileDeletion(
        files: MutableList<out FilePath>,
        exceptions: MutableList<in VcsException?>,
        listener: RollbackProgressListener?
    ) {
        try {
            if (listener != null) {
                LocalUtil.rollbackLocallyDeletedChanges(myFossilVcs.project, files, listener)
            }
        } catch (e: VcsException) {
            exceptions.add(e)
        }
    }
}