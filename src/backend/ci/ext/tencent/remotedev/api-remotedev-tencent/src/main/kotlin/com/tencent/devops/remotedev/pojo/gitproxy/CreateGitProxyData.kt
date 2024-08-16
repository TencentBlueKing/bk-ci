package com.tencent.devops.remotedev.pojo.gitproxy

import com.tencent.devops.common.api.enums.ScmType

data class CreateGitProxyData(
    val gitType: ScmType,
    val projectId: String,
    val url: String,
    val repoName: String,
    val desc: String?,
    val enableLfsCache: Boolean?
)
