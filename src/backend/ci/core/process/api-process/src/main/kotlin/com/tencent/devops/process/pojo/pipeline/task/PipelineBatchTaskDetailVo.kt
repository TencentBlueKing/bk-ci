package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailErrorType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线批量任务明细信息")
data class PipelineBatchTaskDetailVo(
    @get:Schema(description = "任务ID", required = true)
    val taskId: String,
    @get:Schema(description = "项目ID", required = true)
    val projectId: String,
    @get:Schema(description = "任务类型", required = true)
    val taskType: PipelineBatchTaskType,
    @get:Schema(description = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(description = "流水线名称", required = true)
    val pipelineName: String,
    @get:Schema(description = "流水线创建人", required = true)
    val pipelineCreator: String = "",
    @get:Schema(description = "是否开启PAC", required = true)
    val pac: Boolean = false,
    @get:Schema(description = "是否是约束流水线", required = true)
    val constraint: Boolean = false,
    @get:Schema(description = "是否是子流水线添加", required = true)
    val subPipeline: Boolean = false,
    @get:Schema(description = "流水线是否禁用", required = true)
    val locked: Boolean = false,
    @get:Schema(description = "流水线版本状态")
    val versionStatus: VersionStatus? = null,
    @get:Schema(description = "是否修改", required = true)
    val change: Boolean = true,
    @get:Schema(description = "明细状态", required = true)
    val status: PipelineBatchTaskDetailStatus,
    @get:Schema(description = "错误类型")
    val errorType: PipelineBatchTaskDetailErrorType? = null,
    @get:Schema(description = "错误类型名称")
    val errorTypeName: String? = null,
    @get:Schema(description = "错误信息")
    val errorMessageText: Any?,
    @get:Schema(description = "开始时间")
    val startTime: Long?,
    @get:Schema(description = "结束时间")
    val endTime: Long?,
    @get:Schema(description = "创建时间")
    val createTime: Long? = null,
    @get:Schema(description = "更新时间")
    val updateTime: Long? = null
) {
    constructor(task: PipelineBatchTaskDetail) : this(
        taskId = task.taskId,
        projectId = task.projectId,
        taskType = task.taskType,
        pipelineId = task.pipelineId,
        pipelineName = task.pipelineName,
        pipelineCreator = task.pipelineCreator,
        pac = task.pac,
        constraint = task.constraint,
        subPipeline = task.subPipeline,
        locked = task.locked,
        versionStatus = task.versionStatus,
        change = task.change,
        status = task.status,
        errorType = task.errorType,
        errorTypeName = task.errorType?.getI18nName(),
        errorMessageText = task.errorMessage?.errorMessageText(),
        startTime = task.startTime,
        endTime = task.endTime,
        createTime = task.createTime,
        updateTime = task.updateTime
    )
}
