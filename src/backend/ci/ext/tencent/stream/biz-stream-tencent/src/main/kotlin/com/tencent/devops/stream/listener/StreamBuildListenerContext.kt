package com.tencent.devops.stream.listener

import com.tencent.devops.common.api.enums.BuildReviewType
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRepositoryConf
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting

interface StreamBuildListenerContext {
    val buildEvent: BuildEvent
    val requestEvent: GitRequestEvent
    val streamBuildEvent: StreamBuildEvent
    val pipeline: GitProjectPipeline
}

// 获取整体的构建状态
fun StreamBuildListenerContext.isSuccess() =
    (getBuildStatus() == BuildStatus.SUCCEED || getBuildStatus() == BuildStatus.STAGE_SUCCESS)

// 获取commit checkState
fun StreamBuildListenerContext.getGitCommitCheckState(): GitCICommitCheckState {
    // stage审核的状态专门判断为成功
    return when (getBuildStatus()) {
        BuildStatus.REVIEWING -> {
            GitCICommitCheckState.PENDING
        }
        //  审核成功的阶段性状态
        BuildStatus.REVIEW_PROCESSED -> {
            GitCICommitCheckState.PENDING
        }
        else -> {
            if (isSuccess()) {
                GitCICommitCheckState.SUCCESS
            } else {
                GitCICommitCheckState.FAILURE
            }
        }
    }
}

open class StreamBuildListenerContextV2(
    override val buildEvent: BuildEvent,
    override val requestEvent: GitRequestEvent,
    override val streamBuildEvent: StreamBuildEvent,
    override val pipeline: GitProjectPipeline,
    open val streamSetting: GitCIBasicSetting
) : StreamBuildListenerContext

class StreamBuildStageListenerContextV2(
    override val buildEvent: BuildEvent,
    override val requestEvent: GitRequestEvent,
    override val streamBuildEvent: StreamBuildEvent,
    override val pipeline: GitProjectPipeline,
    override val streamSetting: GitCIBasicSetting,
    val reviewType: BuildReviewType
) : StreamBuildListenerContextV2(
    buildEvent, requestEvent, streamBuildEvent, pipeline, streamSetting
)

data class BuildEvent(
    val projectId: String,
    val pipelineId: String,
    val userId: String,
    val buildId: String,
    val status: String,
    val startTime: Long?,
    val stageId: String? = null
)

// 先建立一个Build临时对象，看后续用的多不，多的话拆处集成
data class StreamBuildEvent(
    val id: Long,
    val eventId: Long,
    val pipelineId: String,
    val version: String?,
    val normalizedYaml: String
)

fun StreamBuildEvent.isV2(): Boolean {
    return !version.isNullOrBlank() && version == "v2.0"
}

fun StreamBuildListenerContext.getBuildStatus(): BuildStatus {
    return try {
        BuildStatus.valueOf(buildEvent.status)
    } catch (e: Exception) {
        BuildStatus.UNKNOWN
    }
}
