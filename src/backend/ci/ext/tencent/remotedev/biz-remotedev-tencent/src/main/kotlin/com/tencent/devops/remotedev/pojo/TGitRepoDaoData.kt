package com.tencent.devops.remotedev.pojo

import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoStatus

data class TGitRepoDaoData(
    val tgitId: Long,
    val status: TGitRepoStatus,
    val oauthUser: String,
    val gitType: String,
    val url: String
)
