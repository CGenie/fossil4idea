package org.github.irengrig.fossil4idea.local

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.AbstractVcs
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.VcsVFSListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.intellij.vcsUtil.VcsFileUtil
import org.github.irengrig.fossil4idea.checkin.AddUtil
import org.github.irengrig.fossil4idea.util.FossilUtils
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/23/13
 * Time: 10:30 PM
 */
class FossilVfsListener(vcs: AbstractVcs) : VcsVFSListener(/* vcs = */ vcs) {
    override fun getAddTitle(): String {
        return "Add Files To Fossil"
    }

    override fun getSingleFileAddTitle(): String {
        return "Add File to Fossil"
    }

    override fun getSingleFileAddPromptTemplate(): String {
        return """
            Do you want to add following file to Fossil?
            {0}
            
            If you say No, you can still add it later manually.
            """.trimIndent()
    }

    override fun performAdding(addedFiles: Collection<VirtualFile>, copyFromMap: Map<VirtualFile, VirtualFile>) {
        try {
            AddUtil.scheduleForAddition(
                myProject, ContainerUtil.map(addedFiles
                ) { o -> File(o.path) }
            )
            VcsFileUtil.markFilesDirty(myProject, addedFiles)
        } catch (e: VcsException) {
            myProcessor.addException(e)
        }
    }

    override fun getDeleteTitle(): String {
        return "Delete Files from Fossil"
    }

    override fun getSingleFileDeleteTitle(): String {
        return "Delete File from Fossil"
    }

    override fun getSingleFileDeletePromptTemplate(): String {
        return "Do you want to delete the following file from Fossil?\\n{0}\\n\\nIf you say No, you can still delete it later manually."
    }

    override fun performDeletion(filesToDelete: List<FilePath>) {
        try {
            AddUtil.deleteImpl(myProject, ContainerUtil.map(filesToDelete, FossilUtils.FILE_PATH_FILE_CONVERTOR))
            VcsFileUtil.markFilesDirty(myProject, filesToDelete)
        } catch (e: VcsException) {
            myProcessor.addException(e)
        }
    }

    override fun performMoveRename(movedFiles: List<MovedFileInfo>) {
        for (movedFile in movedFiles) {
            singleMoveRename(movedFile)
        }
    }

    private fun singleMoveRename(movedFile: MovedFileInfo) {
        val oldPath = File(movedFile.myOldPath)
        val newPath = File(movedFile.myNewPath)
        val isRename = FileUtil.filesEqual(oldPath.parentFile, newPath.parentFile)
        val moveWorker: MoveWorker = MoveWorker(myProject)
        try {
            if (isRename) {
                moveWorker.doRename(oldPath, newPath)
            } else {
                moveWorker.doMove(oldPath, newPath.parentFile)
                if (!FileUtil.namesEqual(oldPath.name, newPath.name)) {
                    // + rename
                    moveWorker.doRename(File(newPath.parentFile, oldPath.name), newPath)
                }
            }
        } catch (e: VcsException) {
            myProcessor.addException(e)
        }
    }

    override fun isDirectoryVersioningSupported(): Boolean {
        return false
    }
}