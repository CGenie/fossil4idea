package org.github.irengrig.fossil4idea.util

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.AbstractFilterChildren
import java.io.File
import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/13/13
 * Time: 10:51 PM
 */
class FilterDescendantIoFiles : AbstractFilterChildren<File?>() {
    override fun sortAscending(list: MutableList<out File?>?) {
        Collections.sort(list)
    }

    override fun isAncestor(parent: File?, child: File?): Boolean {
        return FileUtil.isAncestor(parent!!, child!!, false)
    }

    companion object {
        val instance: FilterDescendantIoFiles = FilterDescendantIoFiles()
    }
}