package com.tencent.bkrepo.maven.pojo

data class MavenMetadataSearchPojo(
    val projectId: String,
    val repoName: String,
    val groupId: String,
    val artifactId: String,
    val version: String,
    val classifier: String?,
    val extension: String
)
