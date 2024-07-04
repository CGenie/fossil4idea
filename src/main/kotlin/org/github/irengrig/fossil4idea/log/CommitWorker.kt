package org.github.irengrig.fossil4idea.log

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import org.github.irengrig.fossil4idea.FossilException
import org.github.irengrig.fossil4idea.commandLine.FCommandName
import org.github.irengrig.fossil4idea.commandLine.FossilSimpleCommand
import org.github.irengrig.fossil4idea.local.LocalUtil
import org.github.irengrig.fossil4idea.local.MoveWorker
import org.github.irengrig.fossil4idea.repository.FossilRevisionNumber
import java.io.File
import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/24/13
 * Time: 4:57 PM
 */
class CommitWorker(private val myProject: Project) {
    @Throws(VcsException::class)
    fun getBaseRevisionNumber(file: File): FossilRevisionNumber {
        val baseRevision = getBaseRevision(file)
        val artifactInfo = getArtifactInfo(baseRevision, file)
        return FossilRevisionNumber(baseRevision, artifactInfo.date)
    }

    @Throws(VcsException::class)
    fun getRevisionNumber(file: File, revNum: String): FossilRevisionNumber {
        val artifactInfo = getArtifactInfo(revNum, file)
        return FossilRevisionNumber(revNum, artifactInfo.date)
    }

    @Throws(VcsException::class)
    fun getBaseFileRevision(file: File): FossilFileRevision {
        val baseRevision = getBaseRevision(file)
        val artifactInfo = getArtifactInfo(baseRevision, file)
        return FossilFileRevision(
            myProject, LocalUtil.createFilePath(file), FossilRevisionNumber(baseRevision, artifactInfo.date),
            artifactInfo.user.toString(), artifactInfo.comment.toString()
        )
    }

    @Throws(VcsException::class)
    fun getBaseRevision(file: File): String {
        val command = FossilSimpleCommand(myProject, MoveWorker.findParent(file), FCommandName.finfo)
        command.addParameters("--limit", "1")
        command.addParameters(file.path)
        var result = command.run()!!.trim { it <= ' ' }
        result = result.replace("\r", "")
        val lines = result.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (lines.isEmpty()) {
            throw FossilException("Can not find base revision for " + file.path)
        }
        if (lines[0].startsWith("History of")) {
            // ok
            if (lines.size < 2) throw FossilException(
                """
                    Can not find base revision for ${file.path}
                    :$result
                    """.trimIndent()
            )
            val idx1 = lines[1].indexOf('[')
            val idx2 = lines[1].indexOf(']')
            if (idx1 == -1 || idx2 == -1 || idx1 >= idx2) {
                throw FossilException(
                    """
                        Can not find base revision for ${file.path}
                        :$result
                        """.trimIndent()
                )
            }
            return lines[1].substring(idx1 + 1, idx2)
        } else {
            throw FossilException(result)
        }
    }

    @Throws(VcsException::class)
    fun getArtifactInfo(hash: String, file: File): ArtifactInfo {
        val command = FossilSimpleCommand(myProject, MoveWorker.findParent(file), FCommandName.artifact)
        command.addParameters(hash)
        var result = command.run()
        result = result!!.replace("\r", "")
        val split = result.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val map: MutableMap<Char, String> = HashMap()
        for (s in split) {
            val s1 = s.trim { it <= ' ' }
            map[s1[0]] = s1.substring(2)
        }
        val artifactInfo = ArtifactInfo()
        artifactInfo.hash = hash
        val comment = map['C'] ?: throw FossilException("Cannot find comment in artifact output: $result")
        artifactInfo.comment = comment
        val u = map['U'] ?: throw FossilException("Cannot find user name in artifact output: $result")
        artifactInfo.user = u
        val d = map['D'] ?: throw FossilException("Cannot find date in artifact output: $result")
        val date: Date = DateUtil.parseDate(d) // ?: throw FossilException("Cannot parse date in artifact output: $d")
        artifactInfo.date = date
        val checksum = map['Z'] ?: throw FossilException("Cannot find checksum in artifact output: $result")
        artifactInfo.checkSum = checksum
        return artifactInfo
    } /*c:\fossil\test>fossil artifact 8191
  C "one\smore"
  D 2013-02-24T12:35:49.533
  F 1236.txt 40bd001563085fc35165329ea1ff5c5ecbdbbeef
  F a/aabb.txt f6190088959858b555211616ed50525a353aaaca
  F a/newFile.txt da39a3ee5e6b4b0d3255bfef95601890afd80709
  F a/text.txt da39a3ee5e6b4b0d3255bfef95601890afd80709
  P 628c7cec770e38c2c52b43aec82e194dff4384bc
  R 444f07d947464b09248dfc1f2ac4f64b
  U Irina.Chernushina
  Z 50aa202bcfcc4936e374722dcead9329

  c:\fossil\test>fossil artifact a/aabb.txt
  fossil: not found: a/aabb.txt

  c:\fossil\test>fossil artifact ./a/aabb.txt
  fossil: not found: ./a/aabb.txt

  c:\fossil\test>fossil finfo --limit 1 a/aabb.txt
  History of a/aabb.txt
  2013-02-24 [8191f07a6d] "one more" (user: Irina.Chernushina, artifact:
             [f619008895], branch: trunk)*/
}