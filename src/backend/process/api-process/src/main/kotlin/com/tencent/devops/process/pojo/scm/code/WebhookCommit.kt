package com.tencent.devops.process.pojo.scm.code

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType

data class WebhookCommit(
    val userId: String,
    val pipelineId: String,
    val params: Map<String, Any>,

    val repositoryConfig: RepositoryConfig,
    val repoName: String,
    val commitId: String,
    val block: Boolean,
    val eventType: CodeEventType,
    val codeType: CodeType
)