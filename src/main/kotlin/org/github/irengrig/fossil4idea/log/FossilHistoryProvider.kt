package org.github.irengrig.fossil4idea.log

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.history.*
import com.intellij.openapi.vfs.VirtualFile
import org.github.irengrig.fossil4idea.FossilVcs
import javax.swing.JComponent

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/24/13
 * Time: 7:02 PM
 */
class FossilHistoryProvider(private val myFossilVcs: FossilVcs) : VcsHistoryProvider {
    override fun getUICustomization(
        session: VcsHistorySession,
        forShortcutRegistration: JComponent
    ): VcsDependentHistoryComponents {
        return VcsDependentHistoryComponents.createOnlyColumns(arrayOfNulls(0))
    }

    override fun getAdditionalActions(refresher: Runnable): Array<AnAction> {
        return emptyArray()
    }

    override fun isDateOmittable(): Boolean {
        return false
    }

    override fun getHelpId(): String? {
        return null
    }

    @Throws(VcsException::class)
    override fun createSessionFor(filePath: FilePath): VcsHistorySession? {
        val adapter = VcsAppendableHistoryPartnerAdapter()
        reportAppendableHistory(filePath, adapter)
        adapter.check()

        return adapter.session
    }

    @Throws(VcsException::class)
    override fun reportAppendableHistory(path: FilePath, partner: VcsAppendableHistorySessionPartner) {
        HistoryWorker(myFossilVcs.project).report(path, partner)
    }

    override fun supportsHistoryForDirectories(): Boolean {
        return false
    }

    override fun getHistoryDiffHandler(): DiffFromHistoryHandler? {
        return null
    }

    override fun canShowHistoryFor(file: VirtualFile): Boolean {
        return true
    }
}