package org.github.irengrig.fossil4idea.local

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import org.github.irengrig.fossil4idea.FossilException
import org.github.irengrig.fossil4idea.commandLine.FCommandName
import org.github.irengrig.fossil4idea.commandLine.FossilSimpleCommand
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/23/13
 * Time: 10:49 PM
 */
class MoveWorker(private val myProject: Project) {
    @Throws(VcsException::class)
    fun doRename(oldPath: File, newPath: File) {
        val command = FossilSimpleCommand(myProject, findParent(oldPath), FCommandName.rename)
        command.addParameters(oldPath.path)
        command.addParameters(newPath.name)
        command.run()
    }

    @Throws(VcsException::class)
    fun doMove(oldPath: File, targetDir: File) {
        val command = FossilSimpleCommand(myProject, findParent(oldPath), FCommandName.rename)
        command.addParameters(oldPath.path)
        command.addParameters(targetDir.path)
        command.run()
    }

    companion object {
        @Throws(FossilException::class)
        fun findParent(oldPath: File): File {
            var current: File? = oldPath
            while (current != null) {
                if (current.exists() && current.isDirectory) return current
                current = current.parentFile
            }
            throw FossilException("Can not find existing parent directory for file: " + oldPath.path)
        }
    }
}