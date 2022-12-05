package com.tencent.devops.scm.pojo

import java.time.LocalDateTime

data class WebhookCommit(
    val commitId: String,
    val authorName: String,
    val message: String,
    val repoType: String,
    val commitTime: LocalDateTime,
    var mrId: String?,
    var eventType: String,
    val action: String?
)
