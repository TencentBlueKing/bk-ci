package com.tencent.devops.dispatch.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("TStack构建机配置")
data class TstackConfig(
    @ApiModelProperty("项目 ID", required = true)
    val projectId: String,
    @ApiModelProperty("Tstack构建是否开启", required = true)
    val tstackEnabled: Boolean
)