package org.github.irengrig.fossil4idea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import org.github.irengrig.fossil4idea.FossilVcs
import org.github.irengrig.fossil4idea.ui.UiManager

/**
 * Created by Irina.Chernushina on 5/29/2014.
 */
class RunUIAction : AnAction() {
    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = PlatformDataKeys.PROJECT.getData(e.dataContext) ?: return
        val uiManager: UiManager? = FossilVcs.getInstance(project)!!.getUiManager()
        if (uiManager!!.isRun) {
            e.presentation.setText("Stop Web UI server")
        } else {
            e.presentation.setText("Run Web UI")
        }
    }

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = PlatformDataKeys.PROJECT.getData(anActionEvent.dataContext) ?: return
        val uiManager: UiManager? = FossilVcs.getInstance(project)!!.getUiManager()
        if (uiManager!!.isRun) {
            uiManager.stop()
        } else {
            uiManager.run()
        }
    }
}