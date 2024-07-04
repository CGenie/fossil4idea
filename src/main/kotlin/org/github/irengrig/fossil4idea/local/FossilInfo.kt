package org.github.irengrig.fossil4idea.local

import org.github.irengrig.fossil4idea.repository.FossilRevisionNumber

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/24/13
 * Time: 12:11 AM
 */
class FossilInfo {
    var projectName: String? = null
    var repository: String? = null
    var localPath: String? = null
    var userHome: String? = null
    var projectId: String? = null
    var number: FossilRevisionNumber? = null
    var tags: List<String>? = null
    var comment: String? = null

    constructor()

    constructor(
        projectName: String?, repository: String?, localPath: String?, userHome: String?,
        projectId: String?, number: FossilRevisionNumber?, tags: List<String>?, comment: String?
    ) {
        this.projectName = projectName
        this.repository = repository
        this.localPath = localPath
        this.userHome = userHome
        this.projectId = projectId
        this.number = number
        this.tags = tags
        this.comment = comment
    }
}