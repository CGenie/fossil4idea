package org.github.irengrig.fossil4idea.log

import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.annotate.AnnotationProvider
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.openapi.vfs.VirtualFile
import org.github.irengrig.fossil4idea.FossilException
import org.github.irengrig.fossil4idea.FossilVcs
import org.github.irengrig.fossil4idea.commandLine.FCommandName
import org.github.irengrig.fossil4idea.commandLine.FossilSimpleCommand
import org.github.irengrig.fossil4idea.local.MoveWorker
import org.github.irengrig.fossil4idea.repository.FossilRevisionNumber
import java.io.File
import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/24/13
 * Time: 9:11 PM
 */
class FossilAnnotationProvider(private val myFossilVcs: FossilVcs) : AnnotationProvider {
    @Throws(VcsException::class)
    override fun annotate(file: VirtualFile): FileAnnotation {
        return annotate(file, CommitWorker(myFossilVcs.project).getBaseFileRevision(File(file.path)))
    }

    @Throws(VcsException::class)
    override fun annotate(file: VirtualFile, revision: VcsFileRevision): FileAnnotation {
        val ioFile = File(file.path)
        val annotation = FossilFileAnnotation(
            myFossilVcs.project,
            CatWorker(myFossilVcs.project).cat(ioFile, revision.revisionNumber.asString()).toString(),
            revision.revisionNumber as FossilRevisionNumber, file
        )
        val command = FossilSimpleCommand(myFossilVcs.project, MoveWorker.findParent(ioFile), FCommandName.annotate)
        command.addParameters(ioFile.path)
        var result = command.run()
        result = result!!.replace("\r", "")
        val lines = result.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val commitWorker = CommitWorker(myFossilVcs.project)
        val revisionMap: MutableMap<String, ArtifactInfo> = HashMap()
        var i = 0
        for (line in lines) {
            val spaceIdx = line.indexOf(' ')
            if (spaceIdx == -1) {
                throw FossilException("Can not parse annotation, line: $line")
            }
            val hash = line.substring(0, spaceIdx)
            var artifactInfo = revisionMap[hash]
            if (artifactInfo == null) {
                artifactInfo = commitWorker.getArtifactInfo(hash, ioFile)
                if (artifactInfo == null) {
                    // can not get file information, it was renamed
                    artifactInfo = ArtifactInfo()
                    artifactInfo.hash = hash
                    artifactInfo.date = Date(0)
                } else {
                    revisionMap[hash] = artifactInfo
                }
            }
            annotation.registerLine(i, artifactInfo)
            ++i
        }
        return annotation
    }

    override fun isAnnotationValid(rev: VcsFileRevision): Boolean {
        return true
    }
}