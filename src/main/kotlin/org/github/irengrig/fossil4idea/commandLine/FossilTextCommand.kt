package org.github.irengrig.fossil4idea.commandLine

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/13/13
 * Time: 8:32 PM
 */
abstract class FossilTextCommand(project: Project?, workingDirectory: File?, commandName: FCommandName) :
    FossilCommand(project!!, workingDirectory!!, commandName) {
    private var myIsDestroyed = false
    private var myHandler: OSProcessHandler? = null

    override fun waitForProcess() {
        if (myHandler != null) {
            while (true) {
                if (myHandler!!.waitFor(200)) break
                val pm = ProgressManager.getInstance()
                if (pm.hasProgressIndicator()) {
                    if (pm.progressIndicator.isCanceled) {
                        destroyProcess()
                    }
                }
            }
        }
    }

    @Throws(ExecutionException::class)
    override fun startProcess(): Process? {
        if (myIsDestroyed) return null
        val process = myCommandLine.createProcess()
        myHandler = OSProcessHandler(process, myCommandLine.commandLineString)
        return myHandler!!.process
    }

    override fun startHandlingStreams() {
        if (myIsDestroyed || myProcess == null) return

        myHandler!!.addProcessListener(object : ProcessListener {
            override fun startNotified(event: ProcessEvent) {
                // do nothing
            }

            override fun processTerminated(event: ProcessEvent) {
                val exitCode = event.exitCode
                try {
                    setExitCode(exitCode)
                    //cleanupEnv();   todo
                    this@FossilTextCommand.processTerminated(exitCode)
                } finally {
                    listeners().processTerminated(exitCode)
                }
            }

            override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
                // do nothing
            }
        })
        myHandler!!.startNotify()
    }

    protected abstract fun processTerminated(exitCode: Int)
    protected abstract fun onTextAvailable(text: String?, outputType: Key<*>?)

    override fun destroyProcess() {
        myIsDestroyed = true
        if (myHandler != null) {
            myHandler!!.destroyProcess()
        }
    }
}