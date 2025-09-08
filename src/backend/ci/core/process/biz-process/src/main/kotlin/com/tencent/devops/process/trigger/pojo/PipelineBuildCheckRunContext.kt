package com.tencent.devops.process.trigger.pojo

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.scm.api.pojo.CheckRunInput

/**
 * check run解析后变量的值
 */
data class PipelineBuildCheckRunContext(
    // 构建任务参数
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val buildNum: Int,
    val buildStatus: BuildStatus,
    val buildVariables: Map<String, String> = mapOf(),
    val startTime: Long,
    val pipelineName: String,
    val triggerType: String,
    val channelCode: ChannelCode,
    val buildUrl: String,

    val repoHashId: String,
    val repoType: CodeType,
    val eventType: CodeEventType,
    // checkRun对应的版本
    val commitId: String,
    val context: String,
    // 是否锁定（TGIT专用）
    val block: Boolean? = false,
    val pullRequestId: Long,
    val targetBranches: List<String> = listOf(),
    val externalId: String
)

/**
 * check-run扩展字段
 */
data class CheckRunExtension(
    var checkRunInput: CheckRunInput
)
