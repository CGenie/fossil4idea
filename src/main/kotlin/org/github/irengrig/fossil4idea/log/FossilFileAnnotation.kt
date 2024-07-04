package org.github.irengrig.fossil4idea.log

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.FilePathImpl
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.annotate.AnnotationSourceSwitcher
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vcs.annotate.LineAnnotationAspect
import com.intellij.openapi.vcs.annotate.LineAnnotationAspectAdapter
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.text.DateFormatUtil
import org.github.irengrig.fossil4idea.repository.FossilRevisionNumber
import java.util.*
import kotlin.math.max

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/24/13
 * Time: 9:14 PM
 */
class FossilFileAnnotation(
    private val myProject: Project, private val myContent: String, private val myNumber: FossilRevisionNumber,
    private val myVirtualFile: VirtualFile
) : FileAnnotation(myProject) {
    val myMap: MutableMap<Int, ArtifactInfo> = HashMap()
    private var myMaxIdx = 0

    fun registerLine(number: Int, info: ArtifactInfo) {
        myMap[number] = info
        myMaxIdx = max(number.toDouble(), myMaxIdx.toDouble()).toInt()
    }

    private val DATE: LineAnnotationAspect = object : MyAnnotationAspect(DATE, null) {
        override fun getValue(line: Int): String {
            if (line < 0 || line > myMaxIdx) return ""
            val artifactInfo = myMap[line]
            return if (artifactInfo == null) "" else DateFormatUtil.formatDate(artifactInfo.date!!)
        }
    }
    private val REVISION: LineAnnotationAspect = object : MyAnnotationAspect(REVISION, null) {
        override fun getValue(line: Int): String {
            if (line < 0 || line > myMaxIdx) return ""
            val artifactInfo = myMap[line]
            var hash = artifactInfo!!.hash
            hash = if (hash!!.length > 8) hash.substring(0, 8) else hash
            return if (artifactInfo == null) "" else hash
        }
    }
    private val AUTHOR: LineAnnotationAspect = object : MyAnnotationAspect(AUTHOR, null) {
        override fun getValue(line: Int): String {
            if (line < 0 || line > myMaxIdx) return ""
            val artifactInfo = myMap[line]
            return if (artifactInfo == null) "" else artifactInfo.user!!
        }
    }

    override fun dispose() {
    }

    override fun getAspects(): Array<LineAnnotationAspect> {
        return arrayOf(REVISION, DATE, AUTHOR)
    }

    override fun getToolTip(line: Int): String? {
        if (line < 0 || line > myMaxIdx) return ""
        val artifactInfo = myMap[line]
        return if (artifactInfo == null) "" else artifactInfo.comment
    }

    override fun getAnnotatedContent(): String? {
        return myContent
    }

    override fun getLineRevisionNumber(line: Int): VcsRevisionNumber? {
        if (line < 0 || line > myMaxIdx) return null
        val artifactInfo = myMap[line] ?: return null
        return FossilRevisionNumber(artifactInfo.hash!!, artifactInfo.date)
    }

    override fun getLineDate(line: Int): Date? {
        if (line < 0 || line > myMaxIdx) return null
        val artifactInfo = myMap[line] ?: return null
        return artifactInfo.date
    }

    override fun originalRevision(lineNumber: Int): VcsRevisionNumber? {
        return getLineRevisionNumber(lineNumber)
    }

    override fun getCurrentRevision(): VcsRevisionNumber? {
        return myNumber
    }

    override fun getRevisions(): List<VcsFileRevision>? {
        val artifactInfos = HashSet(myMap.values)
        val result: MutableList<VcsFileRevision> = ArrayList(myMap.size)
        //todo correct filepath
        val fp: FilePath = LocalFilePath(myVirtualFile.path, false)
        for (artifactInfo in artifactInfos) {
            result.add(
                FossilFileRevision(
                    myProject, fp, FossilRevisionNumber(artifactInfo.hash!!, artifactInfo.date),
                    artifactInfo.user.toString(), artifactInfo.comment.toString()
                )
            )
        }
        Collections.sort(
            result
        ) { o1, o2 -> o1.revisionDate.compareTo(o2.revisionDate) }
        return result
    }

    fun revisionsNotEmpty(): Boolean {
        return !myMap.isEmpty()
    }

    override fun getAnnotationSourceSwitcher(): AnnotationSourceSwitcher? {
        return null
    }

    override fun getLineCount(): Int {
        return myMaxIdx + 1 ///todo think
    }

    override fun getFile(): VirtualFile? {
        return myVirtualFile
    }

    private abstract inner class MyAnnotationAspect protected constructor(id: String?, displayName: String?) :
        LineAnnotationAspectAdapter(id, displayName) {
        override fun isShowByDefault(): Boolean {
            return true
        }

        override fun getTooltipText(line: Int): String? {
            if (line < 0 || line > myMaxIdx) return null
            val artifactInfo = myMap[line] ?: return null
            return artifactInfo.comment
        }

        override fun showAffectedPaths(line: Int) {
            // todo
        }
    }
}