package org.github.irengrig.fossil4idea

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import org.jdom.Element
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/12/13
 * Time: 12:04 PM
 */
@State(name = "FossilConfiguration", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
abstract class FossilConfiguration : PersistentStateComponent<Element?> {
    var FOSSIL_PATH: String = ""
    private val myRemoteUrls: MutableMap<File, String> = HashMap()

    override fun getState(): Element? {
        val element = Element("state")
        element.setAttribute("FOSSIL_PATH", FOSSIL_PATH)
        return element
    }

    fun loadState(element: Element?) {
        val fossilPath = element!!.getAttribute("FOSSIL_PATH")
        if (fossilPath != null) {
            FOSSIL_PATH = fossilPath.value
        }
    }

    var remoteUrls: Map<File, String>
        get() = myRemoteUrls
        set(urls) {
            myRemoteUrls.clear()
            myRemoteUrls.putAll(urls)
        }

    companion object {
        fun getInstance(project: Project?): FossilConfiguration {
            // return project.getService(FossilConfiguration::class.java)
            return ServiceManager.getService(project!!, FossilConfiguration::class.java)
        }
    }
}