package com.tencent.devops.remotedev.pojo

import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoStatus

data class TGitRepoDaoData(
    val url: String,
    val status: TGitRepoStatus,
    val oauthUser: String
)
