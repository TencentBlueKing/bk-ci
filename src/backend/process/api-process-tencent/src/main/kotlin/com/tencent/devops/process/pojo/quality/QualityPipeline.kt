package com.tencent.devops.process.pojo.quality

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线模型-质量红线-列表信息")
data class QualityPipeline(
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("流水线名称", required = true)
    var pipelineName: String,
    @ApiModelProperty("流水线描述", required = false)
    var pipelineDesc: String?,
    @ApiModelProperty("流水线任务数量", required = true)
    val taskCount: Int,
    @ApiModelProperty("构建次数", required = true)
    val buildCount: Long,
    @ApiModelProperty("最后构建启动时间", required = false)
    val latestBuildStartTime: Long?,
    @ApiModelProperty("最后构建结束时间", required = false)
    val latestBuildEndTime: Long?
)
