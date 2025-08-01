package com.tencent.devops.metrics.pojo.po

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.pipeline.event.MetricsEvent
import io.swagger.v3.oas.annotations.media.Schema

data class MetricsEventPO(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("data")
    val data: List<Data>,
    @JsonProperty("data_id")
    val dataId: Long
) {
    data class Data(
        @JsonProperty("dimension")
        val dimension: Dimension,
        @JsonProperty("event")
        val event: Event,
        @JsonProperty("event_name")
        val eventName: String,
        @JsonProperty("target")
        @Schema(description = "流水线Id")
        val target: String = "",
        @JsonProperty("timestamp")
        val timestamp: Long?
    )

    data class Dimension(
        val executeCount: Int,
        val domain: String = "CICD",
        val source: String = "BKCI",
        @Schema(description = "事件类型:Normal、Warning")
        val type: String,
        val level: LEVEL,
        val projectId: String,
        val pipelineId: String,
        val pipelineName: String,
        val buildId: String,
        val status: String,
        val eventType: MetricsEvent,
        // 层级维度
        @Schema(description = "触发类型:详见 StartType")
        val trigger: String? = null,
        val triggerUser: String? = null,
        val stageName: String? = null,
        val stageId: String? = null,
        val jobName: String? = null,
        val jobId: String? = null,
        val stepName: String? = null,
        val stepId: String? = null,
        val taskId: String? = null,
        val specialStep: String? = null,
        // 错误维度
        val errorCode: Int? = null,
        val errorType: String? = null,
        val errorMessage: String? = null,
        // git相关维度
        val gitRepoUrl: String? = null,
        val gitType: String? = null,
        val gitBranchName: String? = null,
        val gitEventRrl: String? = null,
        val gitEvent: String? = null,
        // job节点信息维度
        val dispatchType: String? = null,
        val dispatchIdentity: String? = null,
        val dispatchName: String? = null,
        // agent维度
        val nodeType: NodeType? = null,
        val hostName: String? = null,
        val hostIp: String? = null,
        val hostOS: String? = null,
        // job级别互斥组信息
        val jobMutexType: String? = null,
        val mutexGroup: String? = null,
        val agentReuseMutex: String? = null
    )

    enum class NodeType {
        SELF_HOST,
        DEVCLOUD_DOCKER,
        DEVCLOUD_MACOS,
        DEVCLOUD_WINDOWS;

        companion object {
            fun getNodeType(type: String?): NodeType? {
                return when (type) {
                    "SELF_HOST" -> SELF_HOST
                    "DEVCLOUD_DOCKER" -> DEVCLOUD_DOCKER
                    "DEVCLOUD_MACOS" -> DEVCLOUD_MACOS
                    "DEVCLOUD_WINDOWS" -> DEVCLOUD_WINDOWS
                    else -> null
                }
            }
        }
    }

    enum class LEVEL {
        PIPELINE,
        JOB,
        AGENT,
        STAGE,
        STEP
    }

    data class Event(
        @JsonProperty("content")
        val content: String,
        @JsonProperty("extra")
        val extra: Extra
    )

    data class Extra(
        val startTime: Long?,
        val duration: Long?,
        var queueDuration: Long?,
        var reviewDuration: Long?,
        var executeDuration: Long?,
        var systemDuration: Long?
    ) {
        fun check() {
            if (duration != null) {
                queueDuration = queueDuration?.let { if (it < duration) it else 0 }
                reviewDuration = reviewDuration?.let { if (it < duration) it else 0 }
                executeDuration = executeDuration?.let { if (it < duration) it else 0 }
                systemDuration = systemDuration?.let { if (it < duration) it else 0 }
            } else {
                queueDuration = null
                reviewDuration = null
                executeDuration = null
                systemDuration = null
            }
        }
    }

    enum class VARIABLES(val key: String) {
        GIT_CI_REPO_URL("gitRepoUrl"),
        BK_CI_HOOK_TYPE("gitType"),
        GIT_CI_REF("gitBranchName"),
        GIT_CI_EVENT_URL("gitEventRrl"),
        GIT_CI_EVENT("gitEvent"),
    }
}
