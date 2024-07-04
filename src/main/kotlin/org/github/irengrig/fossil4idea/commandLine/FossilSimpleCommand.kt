package org.github.irengrig.fossil4idea.commandLine


import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.ProcessEventListener
import com.intellij.openapi.vcs.VcsException
import org.github.irengrig.fossil4idea.FossilVcs
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/13/13
 * Time: 8:34 PM
 */
class FossilSimpleCommand @JvmOverloads constructor(
    project: Project?, workingDirectory: File?, commandName: FCommandName,
    breakSequence: String? = null
) :
    FossilTextCommand(project, workingDirectory, commandName) {
    val stderr: StringBuilder
    val stdout: StringBuilder
    private val myStartBreakSequence: MutableSet<String>
    private val mySkipErrors: MutableSet<String>
    private val myAnswerYesLines: MutableSet<String>

    init {
        stderr = StringBuilder()
        stdout = StringBuilder()
        myStartBreakSequence = HashSet()
        if (breakSequence != null) {
            myStartBreakSequence.add(breakSequence)
        }
        mySkipErrors = HashSet()
        myAnswerYesLines = HashSet()
        addUsualSequences()
    }

    private fun addUsualSequences() {
        myStartBreakSequence.add(
            "If you have recently updated your fossil executable, you might\n" +
                    "need to run \"fossil all rebuild\" to bring the repository\n" +
                    "schemas up to date."
        )
        myStartBreakSequence.add("database is locked")
    }

    fun addBreakSequence(s: String) {
        myStartBreakSequence.add(s)
    }

    fun addSkipError(s: String) {
        mySkipErrors.add(s)
    }

    fun addAnswerYes(s: String) {
        myAnswerYesLines.add(s)
    }

    override fun processTerminated(exitCode: Int) {
        //
    }

    override fun onTextAvailable(text: String?, outputType: Key<*>?) {
        if (tryToInteractivelyCommunicate(text)) {
            return
        }

        if ((ProcessOutputTypes.STDOUT == outputType)) {
            if (isInBreakSequence(text)) {
                stdout.append(text)
                destroyProcess()
                return
            }
            stdout.append(text)
        } else if ((ProcessOutputTypes.STDERR == outputType)) {
            if (stderr.length == 0 && isInBreakSequence(text)) {
                stderr.append(text)
                destroyProcess()
                return
            }
            stderr.append(text)
        }
    }

    // we use --no-warnings instead
    private fun tryToInteractivelyCommunicate(s: String?): Boolean {
        if (s == null || s.isEmpty()) return false
        for (error: String in myAnswerYesLines) {
            if (s.contains(error) || s.lowercase(Locale.getDefault()).contains(error.lowercase(Locale.getDefault()))) {
                val outputStream: OutputStream = myProcess!!.getOutputStream()
                try {
                    outputStream.write("y\n".toByteArray())
                    outputStream.flush()
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
                return true
            }
        }
        return false
    }

    private fun isInBreakSequence(text: String?): Boolean {
        for (s: String in myStartBreakSequence) {
            if (text != null) {
                if (text.contains(s)) return true
            }
        }
        return false
    }

    @Throws(VcsException::class)
    fun run(): String? {
        val ex = arrayOfNulls<VcsException>(1)
        val result = arrayOfNulls<String>(1)
        addListener(object : ProcessEventListener {
            override fun processTerminated(exitCode: Int) {
                try {
                    if (exitCode == 0 || skipError(stderr.toString())) {
                        result[0] = stdout.toString()
                        LOG.info(myCommandLine.getCommandLineString() + " >>\n" + result[0])
                        println(myCommandLine.getCommandLineString() + " >>\n" + result[0])
                    } else {
                        var msg: String = stderr.toString()
                        if (msg.isEmpty()) {
                            msg = stdout.toString()
                        }
                        if (msg.length == 0) {
                            msg = "Fossil process exited with error code: $exitCode"
                        }
                        LOG.info(myCommandLine.getCommandLineString() + " >>\n" + msg)
                        println(myCommandLine.getCommandLineString() + " >>\n" + msg)
                        ex[0] = VcsException(msg)
                    }
                } catch (t: Throwable) {
                    ex[0] = VcsException(t.toString(), t)
                }
            }

            private fun skipError(s: String?): Boolean {
                if (s == null || s.isEmpty()) return false
                for (error: String in mySkipErrors) {
                    if (s.contains(error) || s.lowercase(Locale.getDefault())
                            .contains(error.lowercase(Locale.getDefault()))
                    ) return true
                }
                return false
            }

            override fun startFailed(exception: Throwable) {
                ex[0] = VcsException(
                    ("Process failed to start (" + myCommandLine.getCommandLineString()) + "): " + exception.toString(),
                    exception
                )
            }
        })
        start()
        if (myProcess != null) {
            waitFor()
        }
        if (ex[0] != null) {
            FossilVcs.getInstance(myProject)!!.checkVersion()
            throw ex[0]!!
        }
        if (result[0] == null) {
            throw VcsException("Svn command returned null: " + myCommandLine.getCommandLineString())
        }
        return result[0]
    }

    companion object {
        private val LOG = Logger.getInstance("#FossilSimpleCommand")
    }
}
