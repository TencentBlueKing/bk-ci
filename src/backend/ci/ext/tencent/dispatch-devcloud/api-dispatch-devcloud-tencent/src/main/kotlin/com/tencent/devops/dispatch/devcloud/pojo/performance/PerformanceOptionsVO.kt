package com.tencent.devops.dispatch.devcloud.pojo.performance

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("devcloud性能基础选项配置")
data class PerformanceOptionsVO(
    @ApiModelProperty("CPU")
    val cpu: Int,
    @ApiModelProperty("内存")
    val memory: Int,
    @ApiModelProperty("磁盘")
    val disk: Int,
    @ApiModelProperty("描述")
    val description: String
)
