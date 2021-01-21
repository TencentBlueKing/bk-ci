package com.tencent.devops.gitci.listener

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.gitci.constant.MQ
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.GitRepositoryConf
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.git.GitEvent

@Event(MQ.EXCHANGE_GITCI_MR_CONFLICT_CHECK_EVENT, MQ.ROUTE_GITCI_MR_CONFLICT_CHECK_EVENT)
data class GitCIMrConflictCheckEvent(
    val token: String,
    val gitRequestEvent: GitRequestEvent,
    val event: GitEvent,
    val path2PipelineExists: Map<String, GitProjectPipeline>,
    val gitProjectConf: GitRepositoryConf,
    // 单位为ms，冲突检查超时时间120s
    var retryTime: Int = 24,
    val delayMills: Int = 5 * 1000,
    // 当前not build库中的ID，方便修改状态
    val notBuildRecordId: Long
)