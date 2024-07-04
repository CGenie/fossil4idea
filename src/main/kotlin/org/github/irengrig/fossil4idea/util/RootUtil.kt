package org.github.irengrig.fossil4idea.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.github.irengrig.fossil4idea.commandLine.FCommandName
import org.github.irengrig.fossil4idea.commandLine.FossilSimpleCommand
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 3/2/13
 * Time: 8:37 PM
 */
object RootUtil {
    private const val ourCheckoutFileName = "_FOSSIL_"

    fun getFossilRoots(roots: Array<VirtualFile?>?): List<VirtualFile> {
        if (roots == null || roots.size == 0) return emptyList()
        val result: MutableList<VirtualFile> = ArrayList()
        for (rootsUnderVc in roots) {
            VfsUtil.processFileRecursivelyWithoutIgnored(
                rootsUnderVc!!
            ) { virtualFile ->
                if (ourCheckoutFileName == virtualFile.name) {
                    result.add(virtualFile.parent)
                }
                true
            }
        }
        return result
    }

    fun getWcRoot(file: File?): File? {
        var current = file
        val lfs = LocalFileSystem.getInstance()
        var virtualFile = lfs.refreshAndFindFileByIoFile(current!!)
        while (current != null) {
            if (virtualFile != null) {
                if (virtualFile.findChild(ourCheckoutFileName) != null) return File(virtualFile.path)
                virtualFile = virtualFile.parent
            } else {
                current = current.parentFile
            }
        }
        return null
    }

    @Throws(VcsException::class)
    fun getRemoteUrl(project: Project?, root: File): String? {
        val working = if (root.isDirectory) root else root.parentFile
        val command = FossilSimpleCommand(project, working, FCommandName.remote_url)
        val text = command.run()!!.trim { it <= ' ' }
        if ("off" == text) return null
        return text
    }
}