package com.tencent.devops.scm.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "github webhook同步请求")
data class GithubWebhookSyncReq(
    val body: String
)
