package org.github.irengrig.fossil4idea.commandLine

import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.LineHandlerHelper
import com.intellij.openapi.vcs.LineProcessEventListener
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.Semaphore
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/13/13
 * Time: 8:37 PM
 */
class FossilLineCommand(project: Project?, workingDirectory: File?, commandName: FCommandName) :
    FossilTextCommand(project, workingDirectory, commandName) {
    /**
     * the partial line from stdout stream
     */
    private val myStdoutLine = StringBuilder()

    /**
     * the partial line from stderr stream
     */
    private val myStderrLine = StringBuilder()
    private val myLineListeners = EventDispatcher.create(LineProcessEventListener::class.java)

    override fun processTerminated(exitCode: Int) {
        // force newline
        if (myStdoutLine.isNotEmpty()) {
            onTextAvailable("\n\r", ProcessOutputTypes.STDOUT)
        } else if (myStderrLine.isNotEmpty()) {
            onTextAvailable("\n\r", ProcessOutputTypes.STDERR)
        }
    }

    override fun onTextAvailable(text: String?, outputType: Key<*>?) {
        val lines: Iterator<String> = LineHandlerHelper.splitText(text).iterator()
        if (ProcessOutputTypes.STDOUT === outputType) {
            notifyLines(outputType, lines, myStdoutLine)
        } else if (ProcessOutputTypes.STDERR === outputType) {
            notifyLines(outputType, lines, myStderrLine)
        }
    }

    private fun notifyLines(outputType: Key<*>?, lines: Iterator<String>, lineBuilder: StringBuilder) {
        if (!lines.hasNext()) return
        if (lineBuilder.isNotEmpty()) {
            lineBuilder.append(lines.next())
            if (lines.hasNext()) {
                // line is complete
                val line = lineBuilder.toString()
                notifyLine(line, outputType)
                lineBuilder.setLength(0)
            }
        }
        while (true) {
            var line: String? = null
            if (lines.hasNext()) {
                line = lines.next()
            }

            if (lines.hasNext()) {
                notifyLine(line, outputType)
            } else {
                if (!line.isNullOrEmpty()) {
                    lineBuilder.append(line)
                }
                break
            }
        }
    }

    private fun notifyLine(line: String?, outputType: Key<*>?) {
        val trimmed = LineHandlerHelper.trimLineSeparator(line)
        myLineListeners.multicaster.onLineAvailable(trimmed, outputType)
    }

    fun addListener(listener: LineProcessEventListener) {
        myLineListeners.addListener(listener)
        super.addListener(listener)
    }

    fun startAndWait(listener: LineProcessEventListener) {
        val semaphore = Semaphore()
        semaphore.down()
        addListener(object : LineProcessEventListener {
            override fun onLineAvailable(s: String, key: Key<*>?) {
                listener.onLineAvailable(s, key)
            }

            override fun processTerminated(i: Int) {
                listener.processTerminated(i)
                semaphore.up()
            }

            override fun startFailed(throwable: Throwable) {
                listener.startFailed(throwable)
                semaphore.up()
            }
        })
        start()
        semaphore.waitFor()
    }
}