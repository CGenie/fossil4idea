package org.github.irengrig.fossil4idea.util

import com.intellij.openapi.vcs.FilePath
import com.intellij.util.containers.Convertor
import java.io.File

/**
 * Created by irengrig on 23.11.2014.
 */
object FossilUtils {
    val FILE_PATH_FILE_CONVERTOR =
        { filePath: FilePath -> filePath.ioFile }

    fun <T> ensureList(coll: Collection<T>): List<T> {
        return if (coll is List<*>) coll as List<T> else ArrayList(coll)
    }
}