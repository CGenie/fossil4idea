package org.github.irengrig.fossil4idea.util

/**
 * Created by Irina.Chernushina on 5/29/2014.
 */
class TextUtil @JvmOverloads constructor(limit: Int = -1) {
    private val myLimit: Int

    init {
        this.myLimit = if (limit < 0) optimal else limit
    }

    fun insertLineCuts(s: String?): String? {
        if (s == null || s.length <= myLimit) return s
        val idx = s.substring(0, myLimit).lastIndexOf(' ')
        if (idx > 0) {
            return """
                ${s.substring(0, idx)}
                ${insertLineCuts(s.substring(idx + 1))}
                """.trimIndent()
        }
        val idx2 = s.indexOf(' ', myLimit)
        if (idx2 == -1) return s
        return """
             ${s.substring(0, idx2)}
             ${insertLineCuts(s.substring(idx2 + 1))}
             """.trimIndent()
    }

    companion object {
        private const val optimal = 100
    }
}