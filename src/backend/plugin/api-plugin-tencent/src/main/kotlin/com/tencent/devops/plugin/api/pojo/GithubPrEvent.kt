package com.tencent.devops.plugin.api.pojo

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.event.pojo.pipeline.IPipelineEvent

/**
 * Github PullRequest
 */
@Event(MQ.EXCHANGE_GIT_COMMIT_CHECK, MQ.ROUTE_GITHUB_PR)
data class GithubPrEvent(
    override val projectId: String,
    override val pipelineId: String,
    val buildId: String,
    val repositoryConfig: RepositoryConfig,
    val commitId: String,
    val status: String,
    val startedAt: Long?,
    val conclusion: String?,
    val completedAt: Long?,
    override var actionType: ActionType = ActionType.REFRESH,
    override val source: String,
    override val userId: String,
    override var delayMills: Int = 0,
    override var retryTime: Int = 3
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills, retryTime)