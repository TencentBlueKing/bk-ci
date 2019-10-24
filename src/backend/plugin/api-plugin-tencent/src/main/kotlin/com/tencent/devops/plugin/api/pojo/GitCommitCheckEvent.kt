package com.tencent.devops.plugin.api.pojo

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.IPipelineEvent

/**
 * TGit提交检查事件
 */
@Event(MQ.EXCHANGE_GIT_COMMIT_CHECK, MQ.ROUTE_GIT_COMMIT_CHECK)
data class GitCommitCheckEvent(
    override val projectId: String,
    override val pipelineId: String,
    val buildId: String,
    val repositoryConfig: RepositoryConfig,
    val commitId: String,
    val state: String,
    val block: Boolean,
    val status: String = "",
    val triggerType: String = "",
    val startTime: Long = 0L,
    val mergeRequestId: Long? = null,
    override var actionType: ActionType = ActionType.REFRESH,
    override val source: String,
    override val userId: String,
    override var delayMills: Int = 0,
    override var retryTime: Int = 3
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills, retryTime)