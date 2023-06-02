package com.tencent.devops.dispatch.devcloud.pojo.performance

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("devcloud项目性能配置")
data class PerformanceConfigVO(
    @ApiModelProperty("蓝盾项目ID")
    val projectId: String,
    @ApiModelProperty("CPU")
    val cpu: Int,
    @ApiModelProperty("内存")
    val memory: String,
    @ApiModelProperty("磁盘")
    val disk: String,
    @ApiModelProperty("描述")
    val description: String
)
