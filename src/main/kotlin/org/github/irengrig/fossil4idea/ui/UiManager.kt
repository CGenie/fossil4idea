package org.github.irengrig.fossil4idea.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.ui.VcsBalloonProblemNotifier
import org.github.irengrig.fossil4idea.commandLine.FCommandName
import org.github.irengrig.fossil4idea.commandLine.FossilSimpleCommand
import java.io.File

/**
 * Created by Irina.Chernushina on 5/29/2014.
 */
class UiManager(private val myProject: Project) {
    private var command: Runner? = null
    private val myLock = Any()

    fun run() {
        synchronized(myLock) {
            if (command != null) return
            ApplicationManager.getApplication().assertIsDispatchThread()
            command = Runner(myProject)
            ApplicationManager.getApplication().executeOnPooledThread(command!!)
        }
    }

    fun stop() {
        synchronized(myLock) {
            if (command == null) return
            ApplicationManager.getApplication().assertIsDispatchThread()
            command!!.stop()
            command = null
        }
    }

    val isRun: Boolean
        get() {
            synchronized(myLock) {
                return command != null
            }
        }

    private class Runner(private val myProject: Project) : Runnable {
        private var myDispose = false
        private var myCommand: FossilSimpleCommand? = null
        private val myLock = Any()

        override fun run() {
            synchronized(myLock) {
                myCommand =
                    FossilSimpleCommand(myProject, File(myProject.guessProjectDir()!!.path), FCommandName.ui)
            }
            try {
                myCommand!!.run()
            } catch (e: VcsException) {
                synchronized(myLock) {
                    if (!myDispose) {
                        VcsBalloonProblemNotifier.showOverVersionControlView(
                            myProject,
                            "Could not run Fossil web UI: " + e.message,
                            MessageType.ERROR
                        )
                    }
                }
            }
        }

        fun stop() {
            val simpleCommand: FossilSimpleCommand?
            synchronized(myLock) {
                myDispose = true
                simpleCommand = myCommand
            }
            simpleCommand?.destroyProcess()
        }
    }
}