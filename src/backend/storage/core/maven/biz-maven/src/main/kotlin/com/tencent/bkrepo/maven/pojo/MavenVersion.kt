package com.tencent.bkrepo.maven.pojo

import com.tencent.bkrepo.maven.constants.SNAPSHOT_SUFFIX
import org.apache.commons.lang3.StringUtils

data class MavenVersion(
    val artifactId: String,
    val version: String,
    var timestamp: String? = null,
    var buildNo: Int? = null,
    var classifier: String? = null,
    val packaging: String
) {
    /**
     * e.g. test-1.0-20211206.112233.jar  >> test-1.0-SNAPSHOT.jar
     */
    fun combineToNonUnique(): String {
        val list = mutableListOf(artifactId, version)
        // 如果为pom 包，是不依赖环境的。
        if (packaging != "pom" && classifier != null) {
            list.add(classifier!!)
        }
        return "${StringUtils.join(list, '-')}.$packaging"
    }

    /**
     * e.g. test-1.0-SNAPSHOT.jar  >>  test-1.0-20211206.112233-1.jar
     */
    fun combineToUnique(): String {
        val list = mutableListOf(artifactId, version.removeSuffix(SNAPSHOT_SUFFIX), timestamp, buildNo)
        if (packaging != "pom" && classifier != null) {
            list.add(classifier!!)
        }
        return "${StringUtils.join(list, '-')}.$packaging"
    }
}
