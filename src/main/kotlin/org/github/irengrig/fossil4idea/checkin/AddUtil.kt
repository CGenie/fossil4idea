package org.github.irengrig.fossil4idea.checkin

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsException
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.JBIterable
import org.github.irengrig.fossil4idea.FossilVcs
import org.github.irengrig.fossil4idea.commandLine.FCommandName
import org.github.irengrig.fossil4idea.commandLine.FossilSimpleCommand
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/14/13
 * Time: 6:56 PM
 */
object AddUtil {
    @Throws(VcsException::class)
    fun scheduleForAddition(project: Project, list: List<File>?) {
        val checkList: MutableList<File> = ArrayList(list)
        val iterator = checkList.iterator()
        while (iterator.hasNext()) {
            val file = iterator.next()
            if (file.isDirectory) {
                iterator.remove()
            }
        }

        val split: List<List<File>> = JBIterable.from(list).split(5).map({l -> l.toList()}).toList()
        for (files in split) {
            addImpl(project, files)
        }
    }

    @Throws(VcsException::class)
    fun deleteImpl(project: Project?, files: List<File>) {
        val parent = tryFindCommonParent(project, files)
        if (parent != null) {
            val command = FossilSimpleCommand(project, parent, FCommandName.delete)
            command.addParameters(
                ContainerUtil.map(files
                ) { o -> FileUtil.getRelativePath(parent, o) }
            )
            val run = command.run()
        } else {
            for (file in files) {
                val command = FossilSimpleCommand(project, file, FCommandName.delete)
                command.addParameters(".")
                command.run()
            }
        }
    }

    @Throws(VcsException::class)
    private fun addImpl(project: Project, files: List<File>) {
        val parent = tryFindCommonParent(project, files)
        if (parent != null) {
            val command = FossilSimpleCommand(project, parent, FCommandName.add)
            command.addParameters("--dotfiles")
            command.addParameters("--force")
            command.addParameters(
                ContainerUtil.map(files
                ) { o -> FileUtil.getRelativePath(parent, o) }
            )
            val run = command.run()
        } else {
            for (file in files) {
                if (file.parentFile == null) continue
                val command = FossilSimpleCommand(project, file.parentFile, FCommandName.add)
                command.addParameters("--dotfiles")
                command.addParameters("--force")
                command.addParameters(file.name)
                command.run()
            }
        }
    }

    fun tryFindCommonParent(project: Project?, files: List<File>): File? {
        if (files.isEmpty()) return null

        //if (files.size() == 1) return files.get(0).getParentFile();
        val other = files.subList(1, files.size)
        val rootsUnderVcs = ProjectLevelVcsManager.getInstance(project!!).getRootsUnderVcs(
            FossilVcs.getInstance(project)!!
        )
        if (rootsUnderVcs == null || rootsUnderVcs.size == 0) return null
        var current = files[0].parentFile
        while (current != null) {
            var ok = true
            for (file in other) {
                if (!FileUtil.isAncestor(current, file, false)) {
                    ok = false
                    break
                }
            }
            if (ok) break
            current = current.parentFile
        }
        if (current != null) {
            for (rootsUnderVc in rootsUnderVcs) {
                if (FileUtil.isAncestor(File(rootsUnderVc.path), current, false)) return current
            }
        }
        return null
    }
}