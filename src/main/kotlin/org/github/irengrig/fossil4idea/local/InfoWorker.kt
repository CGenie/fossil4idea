package org.github.irengrig.fossil4idea.local

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.util.Consumer
import org.github.irengrig.fossil4idea.FossilException
import org.github.irengrig.fossil4idea.commandLine.FCommandName
import org.github.irengrig.fossil4idea.commandLine.FossilSimpleCommand
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/24/13
 * Time: 12:11 AM
 */
class InfoWorker(private val myProject: Project, private val myObj: File, private val myOptional: String?) {
    @get:Throws(VcsException::class)
    val info: FossilInfo
        get() {
            val workingDirectory: File = MoveWorker.findParent(myObj)
            val command = FossilSimpleCommand(myProject, workingDirectory, FCommandName.info)
            if (myOptional != null) {
                command.addParameters(myOptional)
            } else {
                //command.addParameters(myObj.getPath());
            }
            val result = command.run()
            return parse(result)
        }

    /*project-name: <unnamed>
 repository:   c:/
 local-root:   c:/
 user-home:    C:/
 project-code: d641b91ef25f83ba71ac19a4a10
 checkout:     --0d7d2 2013-02-23 19:47:15 UTC
 tags:         trunk
 comment:      initial empty check-in (user: __)*/
    @Throws(FossilException::class)
    private fun parse(result: String?): FossilInfo {
        var result = result
        result = result!!.replace("\r", "\n")
        val split = result.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val map: MutableMap<String, String> = HashMap(8, 1f)
        for (s in split) {
            val i = s.indexOf(":")
            if (i == -1) {
                throw FossilException("Can not parse 'info' output, line: $s")
            }
            map[s.substring(0, i)] = s.substring(i + 1)
        }
        val info = FossilInfo()
        fillLine(map, "project-name", Consumer { s -> info.projectName = s })
        fillLine(map, "repository", Consumer { s -> info.repository = s })
        fillLine(map, "local-root", Consumer { s -> info.localPath = s })
        /*fillLine(map, "user-home", new Consumer<String>() {
      @Override
      public void consume(final String s) {
        info.setUserHome(s);
      }
    });*/
        fillLine(map, "project-code", Consumer { s -> info.projectId = s })
        fillLine(map, "checkout", Consumer {
            // todo parse revision!!!
        })
        fillLine(map, "tags", Consumer {
            // todo parse tags
        })
        fillLine(map, "comment", Consumer { s -> info.comment = s })
        return info
    }

    @Throws(FossilException::class)
    private fun fillLine(info: Map<String, String>, key: String, consumer: Consumer<String>) {
        val value = info[key] ?: throw FossilException("Can not find info line: $key")
        consumer.consume(java.lang.String(value.trim { it <= ' ' }) as String)
    }
}