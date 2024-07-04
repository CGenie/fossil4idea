package org.github.irengrig.fossil4idea.checkout

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckoutProvider
import org.github.irengrig.fossil4idea.FossilVcs
import org.github.irengrig.fossil4idea.actions.CloneAndOpenAction

class FossilCheckoutProvider : CheckoutProvider {
    override fun doCheckout(project: Project, listener: CheckoutProvider.Listener?) {
        CloneAndOpenAction.executeMe(project, listener)
    }

    override fun getVcsName(): String {
        return FossilVcs.DISPLAY_NAME
    }
}