package com.tencent.devops.process.trigger.pojo

import com.tencent.devops.common.pipeline.enums.BuildStatus

data class PipelineBuildCheckRun(
    // 构建任务参数
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val buildNum: Int,
    val buildStatus: BuildStatus,
    val repoHashId: String,
    val context: String,
    val checkRunStatus: PipelineBuildCheckRunStatus? = null,
    val checkRunId: Long? = null,
    // checkRun对应的版本
    val commitId: String,
    val pullRequestId: Long
) {
    constructor(
        checkRunContext: PipelineBuildCheckRunContext
    ) : this(
        projectId = checkRunContext.projectId,
        pipelineId = checkRunContext.pipelineId,
        buildId = checkRunContext.buildId,
        buildNum = checkRunContext.buildNum,
        buildStatus = checkRunContext.buildStatus,
        repoHashId = checkRunContext.repoHashId,
        context = checkRunContext.context,
        checkRunStatus = PipelineBuildCheckRunStatus.PENDING,
        commitId = checkRunContext.commitId,
        pullRequestId = checkRunContext.pullRequestId
    )
}
