package org.github.irengrig.fossil4idea.commandLine

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.ProcessEventListener
import com.intellij.util.EventDispatcher
import com.intellij.util.Processor
import org.github.irengrig.fossil4idea.FossilConfiguration
import org.github.irengrig.fossil4idea.FossilVcs
import org.jetbrains.annotations.NonNls
import java.io.File
import java.io.OutputStream

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/12/13
 * Time: 12:17 PM
 */
abstract class FossilCommand(protected val myProject: Project, workingDirectory: File, commandName: FCommandName) {
    protected val myCommandLine: GeneralCommandLine = GeneralCommandLine()
    private val myWorkingDirectory = workingDirectory
    protected var myProcess: Process? = null
    private val myLock = Any()
    private var myExitCode: Int? = null // exit code or null if exit code is not yet available
    private var myCanceled = false

    private val myListeners = EventDispatcher.create(
        ProcessEventListener::class.java
    )

    private val myInputProcessor: Processor<OutputStream>? = null // The processor for stdin

    init {
        val configuration = FossilConfiguration.getInstance(myProject)
        val path = if (StringUtil.isEmptyOrSpaces(configuration.FOSSIL_PATH)) "fossil" else configuration.FOSSIL_PATH
        myCommandLine.exePath = path
        myCommandLine.setWorkDirectory(workingDirectory)
        myCommandLine.addParameter(commandName.name)
    }

    fun start() {
        synchronized(myLock) {
            checkNotStarted()
            try {
                myProcess = startProcess()
                if (myProcess != null) {
                    startHandlingStreams()
                } else {
                    FossilVcs.getInstance(myProject)!!.checkVersion()
                    //myListeners.multicaster.startFailed(null)
                }
            } catch (t: Throwable) {
                LOG.info(t)
                FossilVcs.getInstance(myProject)!!.checkVersion()
                myListeners.multicaster.startFailed(t)
            }
        }
    }

    /**
     * Wait for process termination
     */
    fun waitFor() {
        checkStarted()
        try {
            if (myInputProcessor != null && myProcess != null) {
                myInputProcessor.process(myProcess!!.outputStream)
            }
        } finally {
            waitForProcess()
        }
    }

    val isCanceled: Boolean
        get() {
            synchronized(myLock) {
                return myCanceled
            }
        }

    fun cancel() {
        synchronized(myLock) {
            myCanceled = true
            checkStarted()
            destroyProcess()
        }
    }

    protected fun setExitCode(code: Int) {
        synchronized(myLock) {
            myExitCode = code
        }
    }

    fun addListener(listener: ProcessEventListener) {
        synchronized(myLock) {
            myListeners.addListener(listener)
        }
    }

    protected fun listeners(): ProcessEventListener {
        synchronized(myLock) {
            return myListeners.multicaster
        }
    }

    fun addParameters(@NonNls vararg parameters: String) {
        synchronized(myLock) {
            checkNotStarted()
            myCommandLine.addParameters(*parameters)
        }
    }

    fun addParameters(parameters: List<String?>?) {
        synchronized(myLock) {
            checkNotStarted()
            myCommandLine.addParameters(parameters!!)
        }
    }

    abstract fun destroyProcess()
    protected abstract fun waitForProcess()

    @Throws(ExecutionException::class)
    protected abstract fun startProcess(): Process?

    /**
     * Start handling process output streams for the handler.
     */
    protected abstract fun startHandlingStreams()

    /**
     * check that process is not started yet
     *
     * @throws IllegalStateException if process has been already started
     */
    private fun checkNotStarted() {
        check(!isStarted) { "The process has been already started" }
    }

    /**
     * check that process is started
     *
     * @throws IllegalStateException if process has not been started
     */
    protected fun checkStarted() {
        check(isStarted) { "The process is not started yet" }
    }

    val isStarted: Boolean
        /**
         * @return true if process is started
         */
        get() {
            synchronized(myLock) {
                return myProcess != null
            }
        }

    companion object {
        private val LOG = Logger.getInstance(
            FossilCommand::class.java.name
        )
    }
}