package org.github.irengrig.fossil4idea

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.CommitExecutor
import com.intellij.openapi.vcs.changes.CommitSession
import org.github.irengrig.fossil4idea.checkin.FossilCheckinEnvironment
import org.jetbrains.annotations.Nls

/**
 * Created by Irina.Chernushina on 6/1/2014.
 */
class FossilCommitAndPushExecutor(private val project: Project) : CommitExecutor {
    @Nls
    override fun getActionText(): String {
        return "Commit and &Push..."
    }

    override fun createCommitSession(): CommitSession {
        (FossilVcs.getInstance(project)!!.createCheckinEnvironment() as FossilCheckinEnvironment?)!!.setPush()
        return CommitSession.VCS_COMMIT
    }
}