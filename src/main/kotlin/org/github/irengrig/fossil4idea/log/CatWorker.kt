package org.github.irengrig.fossil4idea.log

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import org.github.irengrig.fossil4idea.commandLine.FCommandName
import org.github.irengrig.fossil4idea.commandLine.FossilSimpleCommand
import org.github.irengrig.fossil4idea.local.MoveWorker
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/24/13
 * Time: 4:36 PM
 */
class CatWorker(private val myProject: Project) {
    @Throws(VcsException::class)
    fun cat(file: File, revNum: String?): String? {
        val command = FossilSimpleCommand(myProject, MoveWorker.findParent(file), FCommandName.finfo)
        command.addParameters("-p")
        if (revNum != null && "HEAD" != revNum) {
            command.addParameters("-r", revNum)
        }
        command.addParameters(file.path)
        return command.run()
    }
}