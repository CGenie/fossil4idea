package org.github.irengrig.fossil4idea.log

import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 2/24/13
 * Time: 5:09 PM
 */
class ArtifactInfo {
    var hash: String? = null
    var date: Date? = null
    var user: String? = null
    var checkSum: String? = null
    var comment: String? = null

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as ArtifactInfo

        if (if (hash != null) hash != that.hash else that.hash != null) return false

        return true
    }

    override fun hashCode(): Int {
        return if (hash != null) hash.hashCode() else 0
    } /*  / *c:\fossil\test>fossil artifact 8191
    C "one\smore"
    D 2013-02-24T12:35:49.533

    F 1236.txt 40bd001563085fc35165329ea1ff5c5ecbdbbeef
    F a/aabb.txt f6190088959858b555211616ed50525a353aaaca
    F a/newFile.txt da39a3ee5e6b4b0d3255bfef95601890afd80709
    F a/text.txt da39a3ee5e6b4b0d3255bfef95601890afd80709

    P 628c7cec770e38c2c52b43aec82e194dff4384bc
    R 444f07d947464b09248dfc1f2ac4f64b
    U Irina.Chernushina
    Z 50aa202bcfcc4936e374722dcead9329

    D time-and-date-stamp
    T (+|-|*)tag-name artifact-id ?value?
    U user-name
    Z checksum
  */
}