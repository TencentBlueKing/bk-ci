package com.tencent.devops.monitoring.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("NOC业务故障数据")
data class NocNoticeBusData(
    @ApiModelProperty("故障标识", required = true)
    val label: String,
    @ApiModelProperty("故障内容", required = true)
    val value: String
)