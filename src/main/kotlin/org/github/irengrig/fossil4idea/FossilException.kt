package org.github.irengrig.fossil4idea

import com.intellij.openapi.vcs.VcsException

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/13/13
 * Time: 10:20 PM
 *
 * marker exception
 */
class FossilException : VcsException {
    constructor(message: String?) : super(message)

    constructor(throwable: Throwable?, isWarning: Boolean) : super(throwable, isWarning)

    constructor(throwable: Throwable?) : super(throwable)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(message: String?, isWarning: Boolean) : super(message, isWarning)

    constructor(messages: Collection<String?>?) : super(messages!!)
}