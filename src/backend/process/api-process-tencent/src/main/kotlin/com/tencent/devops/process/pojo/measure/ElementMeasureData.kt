package com.tencent.devops.process.pojo.measure

import com.tencent.devops.common.pipeline.enums.BuildStatus
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("度量数据-原子数据")
data class ElementMeasureData(
    @ApiModelProperty("原子id", required = true)
    val id: String,
    @ApiModelProperty("原子名称", required = true)
    val name: String,
    @ApiModelProperty("工程ID", required = true)
    val projectId: String,
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("原子构建结果", required = true)
    val status: BuildStatus,
    @ApiModelProperty("原子构建启动时间", required = true)
    val beginTime: Long,
    @ApiModelProperty("结束时间", required = true)
    val endTime: Long,
    @ApiModelProperty("Element type", required = true)
    val type: String
)