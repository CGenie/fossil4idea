package org.github.irengrig.fossil4idea.local

import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.ChangeListManagerGate
import com.intellij.openapi.vcs.changes.ChangeProvider
import com.intellij.openapi.vcs.changes.ChangelistBuilder
import com.intellij.openapi.vcs.changes.VcsDirtyScope
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.intellij.vcsUtil.VcsUtil
import org.github.irengrig.fossil4idea.FossilVcs
import org.github.irengrig.fossil4idea.util.FilterDescendantIoFiles
import org.github.irengrig.fossil4idea.util.FossilUtils
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/12/13
 * Time: 12:10 PM
 */
class FossilChangeProvider(private val myProject: Project) : ChangeProvider {
    @Throws(VcsException::class)
    override fun getChanges(
        vcsDirtyScope: VcsDirtyScope, changelistBuilder: ChangelistBuilder,
        progressIndicator: ProgressIndicator, changeListManagerGate: ChangeListManagerGate
    ) {
        val dirs = vcsDirtyScope.recursivelyDirtyDirectories
        val dirtyFiles = vcsDirtyScope.dirtyFiles
        for (dirtyFile in dirtyFiles) {
            if (dirtyFile.isDirectory) {
                dirs.add(dirtyFile)
            } else {
                dirs.add(dirtyFile.parentPath)
            }
        }

        val files: List<File> = ContainerUtil.map(dirs, FossilUtils.FILE_PATH_FILE_CONVERTOR)
        FilterDescendantIoFiles().doFilter(files)

        for (root in files) {
            LocalUtil.reportChanges(myProject, root, changelistBuilder)
            // todo and ignored
            val lfs = LocalFileSystem.getInstance()
            LocalUtil.reportUnversioned(myProject, root) { file ->
                val vf = lfs.findFileByIoFile(file!!)
                if (vf != null) {
                    changelistBuilder.processUnversionedFile(VcsUtil.getFilePath(vf))
                }
            }
        }
        val instance = FileDocumentManagerImpl.getInstance()
        val documents = instance.unsavedDocuments
        for (document in documents) {
            val file = instance.getFile(document!!)
            if (file != null) {
                val status = changeListManagerGate.getStatus(file)
                if (status == null || FileStatus.NOT_CHANGED == status) {
                    changelistBuilder.processChange(
                        LocalUtil.createChange(
                            myProject,
                            File(file.path),
                            FileStatus.MODIFIED
                        ), FossilVcs.vcsKey
                    )
                }
            }
        }
    }


    override fun isModifiedDocumentTrackingRequired(): Boolean {
        return true
    }

    override fun doCleanup(virtualFiles: List<VirtualFile>) {
    }
}