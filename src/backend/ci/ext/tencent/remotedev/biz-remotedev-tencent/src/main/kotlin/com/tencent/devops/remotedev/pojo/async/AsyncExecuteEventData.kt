package com.tencent.devops.remotedev.pojo.async

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.remotedev.config.async.AsyncExecuteEventType
import com.tencent.devops.remotedev.pojo.job.PipelineParam

interface AsyncExecuteEventData {
    fun toType(): AsyncExecuteEventType
}

data class AsyncPipelineEvent(
    val userId: String,
    val projectId: String,
    val pipelineId: String,
    val values: Map<String, String>,
    val channelCode: ChannelCode = ChannelCode.BS,
    val buildNo: Int? = null,
    val startType: StartType = StartType.SERVICE
) : AsyncExecuteEventData {
    override fun toType() = AsyncExecuteEventType.ASYNC_PIPELINE
}

data class AsyncJobEndEvent(
    val id: Long
) : AsyncExecuteEventData {
    override fun toType() = AsyncExecuteEventType.ASYNC_JOB_END
}

data class AsyncTGitAclIp(
    val projectId: String,
    val ips: Set<String>,
    val remove: Boolean,
    val tgitId: Long?
) : AsyncExecuteEventData {
    override fun toType() = AsyncExecuteEventType.ASYNC_TGIT_ACL_IP
}

data class AsyncTGitAclUser(
    val projectId: String,
    val tgitId: Long?
) : AsyncExecuteEventData {
    override fun toType() = AsyncExecuteEventType.ASYNC_TGIT_ACL_USER
}

data class AsyncTCloudCfs(
    val pgId: String,
    val ip: String,
    val ruleId: String,
    val region: String,
    val delete: Boolean
) : AsyncExecuteEventData {
    override fun toType() = AsyncExecuteEventType.ASYNC_TCLOUD_CFS
}

data class AsyncJobPipeline(
    val projectId: String,
    val id: Long,
    val param: PipelineParam
) : AsyncExecuteEventData {
    override fun toType() = AsyncExecuteEventType.ASYNC_JOB_PIPELINE
}