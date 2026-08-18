package com.tencent.devops.common.pipeline.pojo.progress

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "构建任务进度明细")
data class BuildTaskProgressDetail(
    @get:Schema(title = "总进度", required = true)
    val progress: BuildTaskProgressSummary,
    @get:Schema(title = "子任务进度", required = false)
    val subtasks: BuildTaskSubtaskProgressGroup? = null,
    @get:Schema(title = "阶段时间线", required = false)
    val timeline: BuildTaskProgressTimeline? = null
)

@Schema(title = "构建任务总进度")
data class BuildTaskProgressSummary(
    @get:Schema(title = "展示标题", required = false)
    val title: String? = null,
    @get:Schema(title = "进度值，范围0到1", required = true)
    val value: Double,
    @get:Schema(title = "进度概览", required = false)
    val summary: String? = null
)

@Schema(title = "构建任务子任务进度组")
data class BuildTaskSubtaskProgressGroup(
    @get:Schema(title = "展示标题", required = false)
    val title: String? = null,
    @get:Schema(title = "完成概览", required = false)
    val summary: String? = null,
    @get:Schema(title = "子任务列表", required = false)
    val items: List<BuildTaskSubtaskProgressItem>? = null
)

@Schema(title = "构建任务子任务进度项")
data class BuildTaskSubtaskProgressItem(
    @get:Schema(title = "子任务名称", required = true)
    val name: String,
    @get:Schema(title = "子任务进度值，范围0到1", required = true)
    val progress: Double,
    @get:Schema(title = "子任务状态", required = true)
    val status: BuildTaskProgressStatus
)

@Schema(title = "构建任务阶段时间线")
data class BuildTaskProgressTimeline(
    @get:Schema(title = "展示标题", required = false)
    val title: String? = null,
    @get:Schema(title = "阶段列表", required = false)
    val items: List<BuildTaskProgressTimelineItem>? = null
)

@Schema(title = "构建任务阶段时间线项")
data class BuildTaskProgressTimelineItem(
    @get:Schema(title = "阶段名称", required = true)
    val name: String,
    @get:Schema(title = "开始时间，ISO 8601 UTC instant", required = true)
    val startTime: String,
    @get:Schema(title = "持续时间，单位毫秒，null表示进行中", required = false)
    val duration: Long? = null
)

@Schema(title = "构建任务子任务状态")
enum class BuildTaskProgressStatus(private val value: String) {
    SUCCEEDED("succeeded"),
    FAILED("failed"),
    RUNNING("running"),
    PENDING("pending");

    @JsonValue
    fun jsonValue(): String = value

    companion object {
        @JvmStatic
        @JsonCreator
        fun from(value: String): BuildTaskProgressStatus {
            return BuildTaskProgressStatus.entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid progress status: $value")
        }
    }
}
