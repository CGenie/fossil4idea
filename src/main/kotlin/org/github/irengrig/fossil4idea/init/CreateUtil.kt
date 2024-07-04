package org.github.irengrig.fossil4idea.init

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.VcsException
import org.github.irengrig.fossil4idea.FossilException
import org.github.irengrig.fossil4idea.commandLine.FCommandName
import org.github.irengrig.fossil4idea.commandLine.FossilSimpleCommand
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/23/13
 * Time: 11:39 PM
 */
class CreateUtil(private val myProject: Project, repoPath: String) {
    private val myRepoPath = File(repoPath)

    var userName: String? = null
        private set
    var password: String? = null
        private set
    var projectId: String? = null
        private set
    var serverId: String? = null
        private set

    init {
        if (myRepoPath.exists()) {
            throw FossilException("Can not create repository at $repoPath.\nFile already exists.")
        }
        if (!myRepoPath.parentFile.exists()) {
            throw FossilException("Can not create repository at $repoPath.\nParent directory does not exist.")
        }
    }

    /*project-id: hash
  server-id:  hash
  admin-user: ___ (initial password is "__")*/
    @Throws(VcsException::class)
    fun createRepository() {
        val fossilCommand = FossilSimpleCommand(myProject, myRepoPath.parentFile, FCommandName.new_)
        fossilCommand.addParameters(myRepoPath.path)
        var result = fossilCommand.run()
        result = result!!.replace("\r", "")
        val split = result.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val lines: MutableList<String> = ArrayList(3)
        for (line in split) {
            if (!StringUtil.isEmptyOrSpaces(line)) {
                lines.add(line.trim { it <= ' ' })
            }
        }

        if (lines.size != 3) {
            throw FossilException("Can not parse 'new' output: '$result'")
        }
        val expectedHeaders = arrayOf("project-id:", "server-id:", "admin-user:")
        for (i in lines.indices) {
            val line = lines[i]
            if (!line.startsWith(expectedHeaders[i])) {
                throw FossilException("Can not parse 'new' output, line #" + (i + 1) + ": '" + result + "'")
            }
        }
        projectId = java.lang.String(lines[0].substring(expectedHeaders[0].length + 1)) as String
        serverId = java.lang.String(lines[1].substring(expectedHeaders[1].length) + 1) as String
        val userPswd = lines[2].substring(expectedHeaders[2].length).trim { it <= ' ' }
        val idxSpace = userPswd.indexOf(' ')
        if (idxSpace == -1) {
            throw FossilException("Can not parse 'new' output, user-password area: '$result'")
        }
        userName = java.lang.String(userPswd.substring(0, idxSpace)) as String
        val quot1 = userPswd.indexOf('"', idxSpace + 1)
        var quot2 = -1
        if (quot1 >= 0) {
            quot2 = userPswd.indexOf('"', quot1 + 1)
        }
        if (quot1 == -1 || quot2 == -1) {
            throw FossilException("Can not parse 'new' output, user-password area: '$result'")
        }
        password = userPswd.substring(quot1 + 1, quot2)
    }

    companion object {
        @Throws(VcsException::class)
        fun openRepoTo(project: Project?, where: File, repo: File) {
            if (where.exists() && !where.isDirectory) {
                throw FossilException("Can not checkout to " + where.path + ". File already exists.")
            }
            if (!repo.exists() || repo.isDirectory) {
                throw FossilException("Can not checkout from " + repo.path)
            }
            if (!where.exists()) {
                where.mkdirs()
            }
            val command = FossilSimpleCommand(project, where, FCommandName.open_)
            //command.addParameters("--latest");
            command.addParameters(repo.path)
            val result = command.run()
        }
    }
}