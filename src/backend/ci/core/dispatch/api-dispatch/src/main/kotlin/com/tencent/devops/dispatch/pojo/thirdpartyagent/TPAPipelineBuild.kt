package com.tencent.devops.dispatch.pojo.thirdpartyagent

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class TPAPipelineReq(
    @get:Schema(title = "agent Hash ID", required = false)
    val agentId: String?,
    @get:Schema(title = "env Hash ID", required = false)
    val envId: String?,
    @get:Schema(title = "第几页", required = false)
    val page: Int?,
    @get:Schema(title = "每页多少条", required = false)
    val pageSize: Int?,
    @get:Schema(title = "开按执行时间", required = false)
    val startTime: Long?,
    @get:Schema(title = "结束执行时间", required = false)
    val endTime: Long?,
    @get:Schema(title = "pipeline ID", required = false)
    val pipelineId: String?,
    @get:Schema(title = "job ID", required = false)
    val jobId: String?,
    @get:Schema(title = "执行人", required = false)
    val creator: String?,
    @get:Schema(title = "状态", required = false)
    val taskStatusList: List<PipelineTaskStatus>?,
    @get:Schema(title = "视图", required = false)
    val view: TPAPipelineBuildView
)

@Schema(title = "第三方构建任务详情")
data class TPAPipelineBuild(
    @get:Schema(title = "流水线ID")
    val pipelineId: String,
    @get:Schema(title = "流水线名称")
    val pipelineName: String,
    @get:Schema(title = "jobId")
    val jobId: String?,
    @get:Schema(title = "job名称")
    val jobName: String?,
    @get:Schema(title = "这个job的构建次数")
    val buildCount: Int,
    @get:Schema(title = "最后构建时间")
    val lastBuildTime: LocalDateTime?,
    @get:Schema(title = "平均耗时")
    val avgTimeInterval: Long?,
    @get:Schema(title = "最后一次构建的containerId")
    val lastContainerId: Long?,
    @get:Schema(title = "stageId")
    val stageId: String?,
    @get:Schema(title = "stageNumb")
    var stageNumb: String?,
    @get:Schema(title = "buildId")
    val buildId: String?,
    @get:Schema(title = "执行次数")
    val executeCount: Int?,
    // 直接从流水线反查，截取一部分
    @get:Schema(title = "buildHistory")
    var buildHistory: TPAPipelineBuildHistory? = null
)

@Schema(title = "历史构建模型")
data class TPAPipelineBuildHistory(
    @get:Schema(title = "启动用户", required = true)
    val userId: String,
    @get:Schema(title = "构建号", required = true)
    val buildNum: Int?,
    @get:Schema(title = "状态", required = true)
    val status: String,
    @get:Schema(title = "总耗时(毫秒)", required = false)
    val totalTime: Long?,
    @get:Schema(title = "运行耗时(毫秒，不包括人工审核时间)", required = false)
    val executeTime: Long?,
    @get:Schema(title = "流水线的执行开始时间", required = true)
    val startTime: Long,
    @get:Schema(title = "流水线的执行结束时间", required = true)
    val endTime: Long?,
    @get:Schema(title = "执行次数")
    val executeCount: Int?
)

data class TPAPipelineBuildCountResp(
    val pipelineCount: Long,
    val jobCount: Long,
    val buildCount: Long,
    val result: Page<TPAPipelineBuild>
)

@Schema(title = "流水线名称与Id")
data class PipelineIdAndName(
    @get:Schema(title = "流水线Id")
    val pipelineId: String,
    @get:Schema(title = "流水线名称")
    val pipelineName: String
)

@Schema(title = "Job名称与Id")
data class JobIdAndName(
    @get:Schema(title = "JobId")
    val jobId: String,
    @get:Schema(title = "job名称")
    val jobName: String
)

enum class TPAPipelineBuildView {
    PIPELINE,
    JOB,
    BUILD,
    ;
}