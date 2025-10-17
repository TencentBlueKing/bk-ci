package com.tencent.devops.remotedev.pojo.gitproxy

data class TGitBindRemotedevData(
    val tGitId: Long,
    val tGitUrl: String,
    val projectIds: List<String>
)
