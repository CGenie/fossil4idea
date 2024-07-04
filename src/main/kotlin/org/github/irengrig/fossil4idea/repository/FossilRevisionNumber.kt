package org.github.irengrig.fossil4idea.repository

import com.intellij.openapi.vcs.history.VcsRevisionNumber
import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/13/13
 * Time: 11:04 PM
 */
class FossilRevisionNumber(// todo special HEAD revision??
    val hash: String, val date: Date?
) : VcsRevisionNumber {
    override fun asString(): String {
        return hash
    }

    override fun compareTo(o: VcsRevisionNumber): Int {
        if (o is FossilRevisionNumber) {
            if (date != null && o.date != null) {
                return date.compareTo(o.date)
            }
        }
        return 0
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as FossilRevisionNumber

        if (hash != that.hash) return false

        return true
    }

    override fun hashCode(): Int {
        return hash.hashCode()
    }

    companion object {
        val UNKNOWN: FossilRevisionNumber = FossilRevisionNumber("0", null)
    }
}