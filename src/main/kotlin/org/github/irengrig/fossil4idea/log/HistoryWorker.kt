package org.github.irengrig.fossil4idea.log

import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.LineProcessEventListener
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.history.*
import org.github.irengrig.fossil4idea.FossilException
import org.github.irengrig.fossil4idea.commandLine.FCommandName
import org.github.irengrig.fossil4idea.commandLine.FossilLineCommand
import org.github.irengrig.fossil4idea.local.MoveWorker
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/24/13
 * Time: 7:06 PM
 */
class HistoryWorker(private val myProject: Project) {
    private val mySb = StringBuilder()
    private var myOk = false
    private var myFirstLine = true
    private var myPath: FilePath? = null

    @Throws(VcsException::class)
    fun report(path: FilePath, partner: VcsAppendableHistorySessionPartner) {
        myPath = path
        val ioFile = path.ioFile
        val command: FossilLineCommand = FossilLineCommand(myProject, MoveWorker.findParent(ioFile), FCommandName.finfo)
        command.addParameters(path.path)
        val session = MySession(
            myProject, ioFile, ArrayList(),
            CommitWorker(myProject).getBaseRevisionNumber(ioFile)
        )
        partner.reportCreatedEmptySession(session)
        val err = StringBuilder()
        command.startAndWait(object : LineProcessEventListener {
            override fun onLineAvailable(line: String, key: Key<*>) {
                if (ProcessOutputTypes.STDOUT == key) {
                    try {
                        parseLine(line.trim { it <= ' ' }, partner)
                    } catch (e: VcsException) {
                        err.append(e.message)
                    }
                } else if (ProcessOutputTypes.STDERR == key) {
                    err.append(line).append('\n')
                }
            }

            override fun processTerminated(exitCode: Int) {
                partner.finished()
            }

            override fun startFailed(exception: Throwable) {
                partner.finished()
            }
        })
        if (err.length > 0) {
            throw FossilException(err.toString())
        }
        if (!myOk) {
            throw FossilException(mySb.toString())
        }
    }

    /*History of a/aabb.txt
  2013-02-24 [8191f07a6d] "one more" (user: Irina.Chernushina, artifact:
             [f619008895], branch: trunk)
  2013-02-24 [dd554bf674] test commit2 (user: Irina.Chernushina, artifact:
             [7581507ad6], branch: trunk)
  2013-02-24 [95ca278a89] test commit (user: Irina.Chernushina, artifact:
             [da39a3ee5e], branch: trunk)*/
    @Throws(VcsException::class)
    private fun parseLine(line: String, partner: VcsAppendableHistorySessionPartner) {
        if (myFirstLine) {
            if (line.startsWith("History of")) {
                myOk = true
                return
            } else {
                mySb.append(line)
            }
            myFirstLine = false
            return
        }
        if (!myOk) {
            mySb.append(line)
            return
        }

        if (mySb.length > 0) mySb.append(' ')
        mySb.append(line)
        if (')' == mySb[mySb.length - 1]) {
            // line complete
            val totalline = mySb.toString()
            mySb.setLength(0)
            doParseLine(totalline, partner)
        }
    }

    @Throws(VcsException::class)
    private fun doParseLine(totalline: String, partner: VcsAppendableHistorySessionPartner) {
        val idxSq1 = totalline.indexOf('[')
        val idxSq2 = totalline.indexOf(']')
        if (idxSq1 == -1 || idxSq2 == -1) throw FossilException("Can not parse history line: $totalline")
        val revNum = java.lang.String(totalline.substring(idxSq1 + 1, idxSq2)) as String
        val left = totalline.substring(idxSq2 + 1)
        val idxRound = left.indexOf('(')
        if (idxRound == -1) throw FossilException("Can not parse history line: $totalline")
        // first symbol is space, amd space before (
        val comment = left.substring(1, idxRound - 1)
        val userPattern = "user: "
        if (userPattern != left.substring(
                idxRound + 1,
                idxRound + 1 + userPattern.length
            )
        ) throw FossilException("Can not parse history line (user:): $totalline")
        val idxNameEnd = left.indexOf(", artifact:", idxRound + 1 + userPattern.length)
        if (idxNameEnd == -1) throw FossilException("Can not parse history line (name end): $totalline")
        val name = left.substring(idxRound + 1 + userPattern.length, idxNameEnd)
        partner.acceptRevision(
            FossilFileRevision(
                myProject, myPath,
                CommitWorker(myProject).getRevisionNumber(myPath!!.ioFile, revNum), name, comment
            )
        )
    }

    private class MySession : VcsAbstractHistorySession {
        private val myProject: Project
        private val myFile: File

        private constructor(project: Project, file: File, revisions: List<VcsFileRevision>) : super(revisions) {
            myProject = project
            myFile = file
        }

        constructor(
            project: Project, file: File, revisions: List<VcsFileRevision>,
            currentRevisionNumber: VcsRevisionNumber
        ) : super(revisions, currentRevisionNumber) {
            myProject = project
            myFile = file
        }

        override fun calcCurrentRevisionNumber(): VcsRevisionNumber? {
            try {
                return CommitWorker(myProject).getBaseRevisionNumber(myFile)
            } catch (e: VcsException) {
                LOG.info(e)
                return null
            }
        }

        override fun copy(): VcsHistorySession {
            return MySession(myProject, myFile, revisionList, currentRevisionNumber)
        }

        override fun getHistoryAsTreeProvider(): HistoryAsTreeProvider? {
            return null
        }

        companion object {
            private val LOG = Logger.getInstance("#org.jetbrains.fossil4idea.log.HistoryWorker.MySession")
        }
    }
}