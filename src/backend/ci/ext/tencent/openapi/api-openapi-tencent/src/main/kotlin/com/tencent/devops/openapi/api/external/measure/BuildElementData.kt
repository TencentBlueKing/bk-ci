package com.tencent.devops.openapi.api.external.measure

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("度量数据-原子数据")
data class BuildElementData(
    @ApiModelProperty("原子id", required = true)
    val id: String = "",
    @ApiModelProperty("原子名称", required = true)
    val name: String = "",
    @ApiModelProperty("工程ID", required = true)
    val projectId: String = "",
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String = "",
    @ApiModelProperty("构建ID", required = true)
    val buildId: String = "",
    @ApiModelProperty("原子构建结果", required = true)
    val status: String = "",
    @ApiModelProperty("原子构建启动时间", required = true)
    val beginTime: Long = 0,
    @ApiModelProperty("结束时间", required = true)
    val endTime: Long = 0,
    @ApiModelProperty("Element type", required = true)
    val type: String = "",
    @ApiModelProperty("原子节点分类", required = false)
    val category: String = "",
    @ApiModelProperty("atomCode", required = false)
    val atomCode: String = "",
    @ApiModelProperty("templateId", required = false)
    val templateId: String? = "",
    @ApiModelProperty("错误类型", required = false)
    val errorType: String? = null,
    @ApiModelProperty("错误码标识", required = false)
    val errorCode: Int? = null,
    @ApiModelProperty("错误信息描述", required = false)
    val errorMsg: String? = null
)