package org.github.irengrig.fossil4idea

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.AbstractVcs
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsKey
import com.intellij.openapi.vcs.annotate.AnnotationProvider
import com.intellij.openapi.vcs.changes.ChangeProvider
import com.intellij.openapi.vcs.changes.CommitExecutor
import com.intellij.openapi.vcs.checkin.CheckinEnvironment
import com.intellij.openapi.vcs.diff.DiffProvider
import com.intellij.openapi.vcs.history.VcsHistoryProvider
import com.intellij.openapi.vcs.rollback.RollbackEnvironment
import com.intellij.openapi.vcs.update.UpdateEnvironment
import org.github.irengrig.fossil4idea.checkin.FossilCheckinEnvironment
import org.github.irengrig.fossil4idea.local.FossilChangeProvider
import org.github.irengrig.fossil4idea.local.FossilRollbackEnvironment
import org.github.irengrig.fossil4idea.local.FossilVfsListener
import org.github.irengrig.fossil4idea.log.FossilAnnotationProvider
import org.github.irengrig.fossil4idea.log.FossilHistoryProvider
import org.github.irengrig.fossil4idea.ui.UiManager

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/12/13
 * Time: 10:35 AM
 */
class FossilVcs(project: Project) : AbstractVcs(project, NAME) {
    private var myChangeProvider: FossilChangeProvider? = null
    private var myVfsListener: FossilVfsListener? = null
    private var uiManager: UiManager? = null
    private var fossilCheckinEnvironment: FossilCheckinEnvironment? = null

    override fun getDisplayName(): String {
        return DISPLAY_NAME
    }

    override fun getConfigurable(): Configurable {
        return FossilConfigurable(myProject)
    }

    override fun activate() {
        myVfsListener = FossilVfsListener(this)
        uiManager = UiManager(myProject)
    }

    override fun deactivate() {
        if (myVfsListener != null) {
            Disposer.dispose(myVfsListener!!)
            myVfsListener = null
            uiManager?.stop()
        }
    }

    fun getUiManager(): UiManager? {
        return uiManager
    }

    override fun getChangeProvider(): ChangeProvider? {
        if (myChangeProvider == null) {
            myChangeProvider = FossilChangeProvider(myProject)
        }
        return myChangeProvider
    }

    public override fun createCheckinEnvironment(): CheckinEnvironment? {
        if (fossilCheckinEnvironment == null) {
            fossilCheckinEnvironment = FossilCheckinEnvironment(this)
        }
        return fossilCheckinEnvironment
    }

    override fun createRollbackEnvironment(): RollbackEnvironment {
        return FossilRollbackEnvironment(this)
    }

    override fun getVcsHistoryProvider(): VcsHistoryProvider {
        return FossilHistoryProvider(this)
    }

    override fun createUpdateEnvironment(): UpdateEnvironment {
        return FossilUpdateEnvironment(this)
    }

    override fun getCommitExecutors(): List<CommitExecutor> {
        return listOf<CommitExecutor>(FossilCommitAndPushExecutor(myProject))
    }

    fun checkVersion() {
        //todo
    }

    override fun getDiffProvider(): DiffProvider {
        return FossilDiffProvider(this)
    }

    override fun getAnnotationProvider(): AnnotationProvider {
        return FossilAnnotationProvider(this)
    }

    companion object {
        var NAME: String = "fossil"
        var DISPLAY_NAME: String = "Fossil"
        val vcsKey: VcsKey = createKey(NAME)

        fun getInstance(project: Project?): FossilVcs? {
            return ProjectLevelVcsManager.getInstance(project!!).findVcsByName(NAME) as FossilVcs?
        }
    }
}