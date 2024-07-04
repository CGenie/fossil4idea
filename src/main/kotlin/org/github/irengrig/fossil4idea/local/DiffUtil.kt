package org.github.irengrig.fossil4idea.local

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.diff.impl.patch.PatchSyntaxException
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import com.intellij.openapi.diff.impl.patch.apply.GenericPatchApplier
import com.intellij.openapi.vcs.VcsException

/**
 * Created by Irina.Chernushina on 5/31/2014.
 */
class DiffUtil {
    @Throws(VcsException::class)
    fun execute(text: String?, patchContent: String, fileName: String): String {
        var patchContent1 = patchContent
        patchContent1 = reversePatch(patchContent1)
        val patchReader = PatchReader(patchContent1)
        try {
            patchReader.parseAllPatches()
        } catch (e: PatchSyntaxException) {
            throw VcsException("Patch syntax exception in: $fileName", e)
        }
        val patches: List<TextFilePatch> = patchReader.textPatches
        if (patches.size != 1) throw VcsException("Not one file patch in provided char sequence in: $fileName")

        val patch = patches[0]
        val applier = GenericPatchApplier(text, patch.hunks)
        if (!applier.execute()) {
            LOG.info("Patch apply problems for: $fileName")
            applier.trySolveSomehow()
        }
        return applier.after
    }

    companion object {
        private val LOG = Logger.getInstance(DiffUtil::class.java)
        fun reversePatch(patchContent: String): String {
            val strings = patchContent.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val sb = StringBuilder()
            var cnt = 0
            var s = strings[cnt]

            while (cnt < strings.size) {
                // context
                while (contextOrHeader(s) && cnt < strings.size) {
                    sb.append(s)
                    if (cnt < (strings.size - 1)) sb.append("\n")
                    ++cnt
                    if (cnt >= strings.size) break
                    s = strings[cnt]
                }

                if (cnt >= strings.size) break

                val minus = StringBuilder()
                val plus = StringBuilder()
                while (cnt < strings.size && (s.startsWith("+") || s.startsWith("-"))) {
                    if (s.startsWith("+")) {
                        minus.append("-").append(s.substring(1))
                        minus.append("\n")
                    } else {
                        plus.append("+").append(s.substring(1))
                        plus.append("\n")
                    }
                    ++cnt
                    if (cnt >= strings.size) break
                    s = strings[cnt]
                }
                sb.append(minus.toString())
                sb.append(plus.toString())
            }
            val `val` = sb.toString()
            // cut \n back if was added in the end
            if (`val`.endsWith("\n")) return `val`.substring(0, `val`.length - 1)
            return `val`
        }

        private fun contextOrHeader(s: String): Boolean {
            if (s.startsWith("+++")) return true
            if (s.startsWith("---")) return true
            return !s.startsWith("+") && !s.startsWith("-")
        }
    }
}