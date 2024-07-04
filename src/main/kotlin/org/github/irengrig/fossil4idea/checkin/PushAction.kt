package org.github.irengrig.fossil4idea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import org.github.irengrig.fossil4idea.checkin.CheckinUtil

/**
 * Created by Irina.Chernushina on 6/1/2014.
 */
class PushAction : AnAction() {
    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = PlatformDataKeys.PROJECT.getData(anActionEvent.dataContext) ?: return

        CheckinUtil(project).push()
    }
}