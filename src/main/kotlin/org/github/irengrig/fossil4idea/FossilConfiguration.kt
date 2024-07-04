package org.github.irengrig.fossil4idea

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import org.github.irengrig.fossil4idea.fossil.FossilConfigurationState
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/12/13
 * Time: 12:04 PM
 */
@State(name = "FossilConfiguration", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
abstract class FossilConfiguration : PersistentStateComponent<FossilConfigurationState> {
    var FOSSIL_PATH: String = ""
    private val myRemoteUrls: MutableMap<File, String> = HashMap()
    private var myState = FossilConfigurationState()

    override fun getState(): FossilConfigurationState {
        return this.myState
    }

    override fun loadState(state: FossilConfigurationState) {
        this.myState = state
    }

    var remoteUrls: Map<File, String>
        get() = myRemoteUrls
        set(urls) {
            myRemoteUrls.clear()
            myRemoteUrls.putAll(urls)
        }

    companion object {
        fun getInstance(project: Project): FossilConfiguration {
            return project.getService(FossilConfiguration::class.java)
        }
    }
//    companion object {
//        fun getInstance(project: Project?): FossilConfiguration {
//            // return project.getService(FossilConfiguration::class.java)
//            return ServiceManager.getService(project!!, FossilConfiguration::class.java)
//        }
//    }
}