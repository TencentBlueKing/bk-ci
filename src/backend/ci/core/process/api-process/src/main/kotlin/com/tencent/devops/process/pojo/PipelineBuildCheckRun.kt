package com.tencent.devops.process.pojo

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.scm.api.enums.CheckRunStatus
import com.tencent.devops.scm.api.enums.ScmProviderCodes
import com.tencent.devops.scm.api.pojo.CheckRunInput

data class PipelineBuildCheckRun(
    // 构建任务参数
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val buildNum: Int,
    val buildStatus: BuildStatus,
    val repoHashId: String,
    val context: String,
    val checkRunStatus: CheckRunStatus? = null,
    val checkRunId: Long? = null,
    // checkRun对应的版本
    val commitId: String,
    // 扩展信息（存表）
    val extensionData: CheckRunExtension? = null,
    // 是否锁定（TGIT专用）
    val block: Boolean? = false,
    val pullRequestId: Long,
    val pullRequestBizId: String = "",
    val scmCode: String,
    val buildVariables: Map<String, String> = mapOf(),
    val repoType: CodeType,
    val eventType: CodeEventType,
    val startTime: Long? = null,
    val pipelineName: String,
    val triggerType: String,
    val channelCode: ChannelCode,
    val repositoryConfig: RepositoryConfig,
    // 流水线回写checkRun 同步锁
    val lockKey: String,
    val externalId: String,
    val repoProvider: ScmProviderCodes,
    val targetBranches: List<String> = listOf()
)

/**
 * check-run扩展字段
 */
data class CheckRunExtension(
    var checkRunInput: CheckRunInput
)
