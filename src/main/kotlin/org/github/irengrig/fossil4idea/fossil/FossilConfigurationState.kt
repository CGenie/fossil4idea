// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.github.irengrig.fossil4idea.fossil

//import com.intellij.util.PlatformUtils
import com.intellij.util.xmlb.annotations.*
//import sun.awt.X11.Depth

class FossilConfigurationState {
//    @Property(surroundWithTag = false)
//    var directory: ConfigurationDirectory = ConfigurationDirectory()

    var runUnderTerminal: Boolean = false

    @Attribute("myAutoUpdateAfterCommit")
    var autoUpdateAfterCommit: Boolean = false

}