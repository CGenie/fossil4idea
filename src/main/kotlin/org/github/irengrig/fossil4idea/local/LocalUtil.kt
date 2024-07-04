package org.github.irengrig.fossil4idea.local

import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.LineProcessEventListener
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.actions.VcsContextFactory
import com.intellij.openapi.vcs.changes.*
import com.intellij.openapi.vcs.rollback.RollbackProgressListener
import com.intellij.util.Consumer
import com.intellij.util.containers.ContainerUtil
import org.github.irengrig.fossil4idea.FossilException
import org.github.irengrig.fossil4idea.FossilVcs
import org.github.irengrig.fossil4idea.checkin.AddUtil
import org.github.irengrig.fossil4idea.commandLine.FCommandName
import org.github.irengrig.fossil4idea.commandLine.FossilLineCommand
import org.github.irengrig.fossil4idea.commandLine.FossilSimpleCommand
import org.github.irengrig.fossil4idea.log.CommitWorker
import org.github.irengrig.fossil4idea.repository.FossilContentRevision
import org.github.irengrig.fossil4idea.util.FossilUtils
import org.github.irengrig.fossil4idea.util.RootUtil.getWcRoot
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/13/13
 * Time: 8:44 PM
 */
object LocalUtil {
    @Throws(VcsException::class)
    fun reportChanges(
        project: Project?, directory: File?,
        clb: ChangelistBuilder
    ) {
        val lineParser = LineParser(project, clb, directory)
        val err = StringBuilder()
        askChanges(project, directory, lineParser, err)
        if (lineParser.hasSomethingForDiff()) {
            askDiff(project, directory, lineParser, err)
            val changes = lineParser.diffedChanges
            for (change in changes) {
                clb.processChange(change, FossilVcs.vcsKey)
            }
        }
        if (err.length > 0) {
            throw FossilException(err.toString())
        }
    }

    private fun askDiff(project: Project?, directory: File?, lineParser: LineParser, err: StringBuilder) {
        val command: FossilLineCommand = FossilLineCommand(project, directory, FCommandName.diff)
        command.startAndWait(object : LineProcessEventListener {
            override fun onLineAvailable(s: String, key: Key<*>) {
                if (ProcessOutputTypes.STDOUT == key) {
                    try {
                        lineParser.parseDiffLine(s)
                    } catch (e: FossilException) {
                        err.append(e.message)
                    }
                } else if (ProcessOutputTypes.STDERR == key) {
                    err.append(s).append('\n')
                }
            }

            override fun processTerminated(i: Int) {
            }

            override fun startFailed(throwable: Throwable) {
            }
        })
    }

    private fun askChanges(project: Project?, directory: File?, lineParser: LineParser, err: StringBuilder) {
        val command: FossilLineCommand = FossilLineCommand(project, directory, FCommandName.changes)
        command.startAndWait(object : LineProcessEventListener {
            override fun onLineAvailable(s: String, key: Key<*>) {
                if (ProcessOutputTypes.STDOUT == key) {
                    try {
                        lineParser.parseChangesLine(s)
                    } catch (e: FossilException) {
                        err.append(e.message)
                    }
                } else if (ProcessOutputTypes.STDERR == key) {
                    err.append(s).append('\n')
                }
            }

            override fun processTerminated(i: Int) {
            }

            override fun startFailed(throwable: Throwable) {
            }
        })
    }

    @Throws(FossilException::class)
    fun reportUnversioned(project: Project?, directory: File?, consumer: Consumer<File?>) {
        val command: FossilLineCommand = FossilLineCommand(project, directory, FCommandName.extras)
        command.addParameters("--dotfiles")
        val err = StringBuilder()
        command.startAndWait(object : LineProcessEventListener {
            override fun onLineAvailable(s: String, key: Key<*>) {
                if (ProcessOutputTypes.STDOUT == key) {
                    val line = s.trim { it <= ' ' }
                    consumer.consume(File(directory, line))
                } else if (ProcessOutputTypes.STDERR == key) {
                    err.append(s).append('\n')
                }
            }

            override fun processTerminated(exitCode: Int) {
            }

            override fun startFailed(exception: Throwable) {
            }
        })
        if (err.length > 0) {
            throw FossilException(err.toString())
        }
    }

    private val ourWithDiffTypes: MutableSet<String> = HashSet()

    init {
        ourWithDiffTypes.add("EDITED")
        ourWithDiffTypes.add("RENAMED")
    }

    private val ourOneSideTypes: MutableMap<String, FileStatus> = HashMap(7)

    init {
        ourOneSideTypes["ADDED"] = FileStatus.ADDED
        ourOneSideTypes["DELETED"] = FileStatus.DELETED
    }

    @Throws(FossilException::class)
    fun createChange(project: Project?, file: File, changeTypeEnum: FileStatus): Change {
        return Change(createBefore(project, file, changeTypeEnum), createAfter(file, changeTypeEnum))
    }

    @Throws(FossilException::class)
    private fun createBefore(project: Project?, file: File, changeTypeEnum: FileStatus): ContentRevision? {
        if (FileStatus.ADDED == changeTypeEnum) {
            return null
        }
        try {
            return FossilContentRevision(
                project!!,
                createFilePath(file),
                CommitWorker(project).getBaseRevisionNumber(file)
            )
        } catch (e: VcsException) {
            if (e is FossilException) throw e
            throw FossilException(e)
        }
    }

    private fun createAfter(file: File, changeTypeEnum: FileStatus): ContentRevision? {
        if (FileStatus.DELETED == changeTypeEnum) {
            return null
        }
        val filePath = createFilePath(file)
        if (filePath.fileType != null && !filePath.isDirectory && filePath.fileType.isBinary) {
            return CurrentBinaryContentRevision(filePath)
        }
        return CurrentContentRevision(filePath)
    }

    // seems that folders are not versioned
    fun createFilePath(file: File): FilePath {
        if (!file.exists()) {
            return VcsContextFactory.getInstance().createFilePathOn(file, false)
        }
        return VcsContextFactory.getInstance().createFilePathOn(file)
    }

    @Throws(VcsException::class)
    fun rollbackChanges(project: Project, changes: List<Change?>?, listener: RollbackProgressListener) {
        val files = ChangesUtil.getIoFilesFromChanges(changes!!)
        rollbackFiles(project, listener, files)
    }

    // @Throws(FCommandName::class)
    private fun rollbackFiles(project: Project, listener: RollbackProgressListener, files: List<File>) {
        val parent: File? = AddUtil.tryFindCommonParent(project, files)
        if (parent != null) {
            val command = FossilSimpleCommand(project, parent, FCommandName.revert)
            for (file in files) {
                command.addParameters(file.path)
            }
            command.run()
        } else {
            for (file in files) {
                val command = FossilSimpleCommand(project, MoveWorker.findParent(file), FCommandName.revert)
                command.addParameters(file.path)
                command.run()
                listener.accept(file)
            }
        }
    }

    @Throws(VcsException::class)
    fun rollbackLocallyDeletedChanges(project: Project, files: List<FilePath>, listener: RollbackProgressListener) {
        rollbackFiles(project, listener, ContainerUtil.map(files, FossilUtils.FILE_PATH_FILE_CONVERTOR))
    }

    private class LineParser(
        private val myProject: Project?,
        private val myClb: ChangelistBuilder,
        private val myBase: File?
    ) {
        // for diff
        private val myPathsForDiff: MutableSet<String> = HashSet()
        private var myInsideDiff = false
        private var myLastDiffHeaderLine = 0
        private val myPatches: MutableMap<String?, StringBuilder?> = HashMap()
        private var myPreviousPatchName: String? = null
        private var myCurrentFile: String? = null
        private val myBuff = StringBuilder()

        @Throws(FossilException::class)
        fun parseChangesLine(s: String) {
            val line = s.trim { it <= ' ' }
            val spaceIdx = line.indexOf(' ')
            if (spaceIdx == -1) throw FossilException("Can not parse status line: '$s'")
            val typeName = line.substring(0, spaceIdx)
            val type = ourOneSideTypes[typeName]
            val file = File(myBase, line.substring(spaceIdx).trim { it <= ' ' })
            if (type != null) {
                myClb.processChange(createChange(myProject, file, type), FossilVcs.vcsKey)
                return
            }
            if ("MISSING" == typeName) {
                myClb.processLocallyDeletedFile(
                    VcsContextFactory.getInstance().createFilePathOn(file, false)
                )
                return
            }
            if ("CONFLICT" == typeName) {
                myClb.processChange(
                    createChange(myProject, file, FileStatus.MERGED_WITH_CONFLICTS),
                    FossilVcs.vcsKey
                )
                return
            }
            if (ourWithDiffTypes.contains(typeName)) {
                myPathsForDiff.add(s)
            }
            // suppress for now
//      throw new FossilException("Can not parse status line: '" + s + "'");
        }

        fun hasSomethingForDiff(): Boolean {
            return !myPathsForDiff.isEmpty()
        }

        @Throws(FossilException::class)
        fun parseDiffLine(s: String) {
            if (myInsideDiff && myLastDiffHeaderLine >= 0) {
                var isNext = false
                if (myLastDiffHeaderLine == 0) {
                    isNext = s.startsWith("==================")
                } else if (myLastDiffHeaderLine == 1) {
                    isNext = s.startsWith("--- ") && s.length > 4
                } else if (myLastDiffHeaderLine == 2) {
                    isNext = s.startsWith("+++ ") && s.length > 4
                }
                if (isNext) {
                    myBuff.append(s).append("\n")
                    ++myLastDiffHeaderLine
                    if (myLastDiffHeaderLine == 3) {
                        // end of header
                        myLastDiffHeaderLine = -1
                        myPatches[myCurrentFile]!!.append(myBuff)
                        myBuff.setLength(0)
                    }
                } else {
                    // it all was just part of previous patch!
                    if (myPreviousPatchName == null || myPatches[myPreviousPatchName] == null) throw FossilException("Can not parse patch - no header")
                    val stringBuilder = myPatches[myPreviousPatchName]
                    stringBuilder!!.append(myBuff)
                    myBuff.setLength(0)
                    myLastDiffHeaderLine = -1
                }
                return
            }
            if (s.startsWith(INDEX)) {
                myInsideDiff = true
                myLastDiffHeaderLine = 0
                myPreviousPatchName = myCurrentFile
                myCurrentFile = s.substring(INDEX.length).trim { it <= ' ' }
                val sb = StringBuilder()
                myPatches[myCurrentFile] = sb
                sb.append(s)
                return
            }
            if (s.startsWith("ADDED") || s.startsWith("DELETED")) {
                // skip;
                myInsideDiff = false
                myLastDiffHeaderLine = -1
                return
            }
            if (myInsideDiff) {
                myPatches[myCurrentFile]!!.append(s).append("\n")
            }
            // what is it if...?
        }

        @get:Throws(VcsException::class)
        val diffedChanges: List<Change>
            get() {
                var wcRoot = getWcRoot(myBase)
                wcRoot = wcRoot ?: myBase
                val result: MutableList<Change> = ArrayList()
                for ((key, value) in myPatches) {
                    val file = File(wcRoot, key!!.trim { it <= ' ' })
                    val after = createAfter(file, FileStatus.MODIFIED)
                    val newContent = DiffUtil().execute(after!!.content, value.toString(), file.name)
                    val before: ContentRevision = SimpleContentRevision(newContent, createFilePath(file), "Local")
                    result.add(Change(before, after))
                }
                return result
            }

        companion object {
            const val INDEX: String = "Index: "
        }
    }
}