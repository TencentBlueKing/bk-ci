package com.tencent.bkrepo.maven.model

import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document

/**
 * e.g. /bkrepo/maven-test/com/mycompany/app/my-app/1.0-SNAPSHOT/my-app-1.0-20211202.062544-2-sources.jar
 * [projectId] = bkrepo
 * [repoName] = maven-test
 * [groupId] =com.mycompany.app
 * [artifactId] = my-app
 * [version] = 1.0-SNAPSHOT
 * [timestamp] = 20211202.062544
 * [buildNo] = 2
 * [classifier] = sources
 * [extension] = jar
 */
@Document("maven_metadata")
@CompoundIndexes(
    CompoundIndex(
        name = "unique_index",
        def = "{'projectId':1, 'repoName':1, 'groupId':1, 'artifactId':1, " +
            "'version':1, 'classifier':1, 'extension':1 }",
        background = true,
        unique = true
    )
)
data class TMavenMetadataRecord(
    val id: String?,
    val projectId: String,
    val repoName: String,
    val groupId: String,
    val artifactId: String,
    val version: String,
    val timestamp: String?,
    // 为空会导致inc 报错，以0代替null。
    val buildNo: Int,
    val classifier: String?,
    val extension: String
)
