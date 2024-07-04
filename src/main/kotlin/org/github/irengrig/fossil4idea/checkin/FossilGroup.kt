package org.github.irengrig.fossil4idea.actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.AbstractVcs
import com.intellij.openapi.vcs.actions.StandardVcsGroup
import org.github.irengrig.fossil4idea.FossilVcs

class FossilGroup : StandardVcsGroup() {
    override fun getVcs(project: Project): AbstractVcs {
        return FossilVcs.getInstance(project)!!
    }

    override fun getVcsName(project: Project): String? {
        return FossilVcs.NAME
    }
}