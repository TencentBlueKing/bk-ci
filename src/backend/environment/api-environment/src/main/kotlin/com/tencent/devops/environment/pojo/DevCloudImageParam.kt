package com.tencent.devops.environment.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("DevCloud制作镜像参数")
data class DevCloudImageParam(
    @ApiModelProperty("镜像名", required = true)
    val name: String,
    @ApiModelProperty("镜像TAG", required = true)
    val tag: String,
    @ApiModelProperty("描述", required = true)
    val description: String?
)