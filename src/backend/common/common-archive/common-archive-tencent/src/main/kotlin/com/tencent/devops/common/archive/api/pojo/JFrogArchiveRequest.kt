package com.tencent.devops.common.archive.api.pojo

data class JFrogArchiveRequest(
    val path: String,
    val repoKey: String,
    val text: String,
    val repoType: String = "local",
    val trashcan: Boolean = false,
    val type: String = "junction"
)