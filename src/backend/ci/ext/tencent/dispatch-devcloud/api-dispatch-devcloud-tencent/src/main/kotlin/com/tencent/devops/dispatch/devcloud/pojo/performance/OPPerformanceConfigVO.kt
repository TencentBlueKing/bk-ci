package com.tencent.devops.dispatch.devcloud.pojo.performance

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("devcloud项目性能配置")
data class OPPerformanceConfigVO(
    @ApiModelProperty("蓝盾项目ID")
    val projectId: String,
    @ApiModelProperty("性能基础配置ID")
    val optionId: Long
)
