package org.github.irengrig.fossil4idea.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class AskQuestionAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        BrowserUtil.browse("https://stackoverflow.com/questions/ask")
    }
}