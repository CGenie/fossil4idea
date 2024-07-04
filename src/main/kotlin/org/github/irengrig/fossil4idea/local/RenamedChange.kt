package org.github.irengrig.fossil4idea.local

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/23/13
 * Time: 10:07 PM
 */
class RenamedChange(afterRevision: ContentRevision?) :
    Change(afterRevision, afterRevision) {
    override fun getType(): Type {
        return Type.MOVED
    }

    override fun getFileStatus(): FileStatus {
        return FileStatus.MODIFIED
    }

    override fun isRenamed(): Boolean {
        return false
    }

    override fun isMoved(): Boolean {
        return true
    }

    override fun getMoveRelativePath(project: Project): String {
        return "[unknown place]"
    }
}