package com.tencent.devops.stream.listener.buildFinish

import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRepositoryConf
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting

interface StreamFinishContext {
    val buildFinishEvent: PipelineBuildFinishBroadCastEvent
    val requestEvent: GitRequestEvent
    val streamBuildEvent: StreamBuildEvent
    val pipeline: GitProjectPipeline
    val buildStatus: BuildStatus
    val gitStatus: GitCICommitCheckState
}

// 获取commit checkState
fun StreamFinishContext.getGitCommitCheckState(): GitCICommitCheckState {
    return if (buildStatus.isSuccess()) {
        GitCICommitCheckState.SUCCESS
    } else {
        GitCICommitCheckState.FAILURE
    }
}

// TODO: 后续将这个上下文和其他上下文重构集成，并看v1是否可以干掉
data class StreamFinishContextV2(
    override val buildFinishEvent: PipelineBuildFinishBroadCastEvent,
    override val requestEvent: GitRequestEvent,
    override val streamBuildEvent: StreamBuildEvent,
    override val pipeline: GitProjectPipeline,
    override val buildStatus: BuildStatus = BuildStatus.valueOf(buildFinishEvent.status),
    override val gitStatus: GitCICommitCheckState = if (buildStatus.isSuccess()) {
        GitCICommitCheckState.SUCCESS
    } else {
        GitCICommitCheckState.FAILURE
    },
    val streamSetting: GitCIBasicSetting
) : StreamFinishContext

data class StreamFinishContextV1(
    override val buildFinishEvent: PipelineBuildFinishBroadCastEvent,
    override val requestEvent: GitRequestEvent,
    override val streamBuildEvent: StreamBuildEvent,
    override val pipeline: GitProjectPipeline,
    override val buildStatus: BuildStatus = BuildStatus.valueOf(buildFinishEvent.status),
    override val gitStatus: GitCICommitCheckState = if (buildStatus.isSuccess()) {
        GitCICommitCheckState.SUCCESS
    } else {
        GitCICommitCheckState.FAILURE
    },
    val streamSetting: GitRepositoryConf
) : StreamFinishContext

// 先建立一个Build临时对象，看后续用的多不，多的话拆处集成
data class StreamBuildEvent(
    val id: Long,
    val eventId: Long,
    val pipelineId: String,
    val version: String?,
    val normalizedYaml: String
)

fun StreamBuildEvent.isV2(): Boolean {
    return version.isNullOrBlank() && version == "v2.0"
}
