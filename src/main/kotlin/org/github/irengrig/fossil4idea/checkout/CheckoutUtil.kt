package org.github.irengrig.fossil4idea.checkout

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import org.github.irengrig.fossil4idea.commandLine.FCommandName
import org.github.irengrig.fossil4idea.commandLine.FossilSimpleCommand
import java.io.File

/**
 * Created by Irina.Chernushina on 1/30/14.
 */
class CheckoutUtil(private val myProject: Project) {
    @Throws(VcsException::class)
    fun cloneRepo(url: String, localPath: String) {
        val file = File(localPath)
        val command = FossilSimpleCommand(myProject, file.parentFile, FCommandName.clone)
        command.addParameters(url, localPath)
        command.run()
    }

    @Throws(VcsException::class)
    fun checkout(repo: File, target: File?, hash: String?) {
        val command = FossilSimpleCommand(myProject, target, FCommandName.open_)
        command.addParameters(repo.absolutePath)
        if (hash != null) {
            command.addParameters(hash)
        }
        command.run()
    }

    @Throws(VcsException::class)
    fun initRepository(repo: File) {
        val parentFile = repo.parentFile
        if (parentFile.exists() && !parentFile.isDirectory) {
            throw VcsException("Can not create Fossil repository, " + parentFile.path + " is not a directory.")
        }
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw VcsException("Can not create Fossil repository, can not create parent directory: " + parentFile.path)
        }
        val command = FossilSimpleCommand(myProject, parentFile, FCommandName.init_)
        command.addParameters(repo.name)
        command.run()
    }
}