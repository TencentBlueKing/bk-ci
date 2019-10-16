package com.tencent.devops.environment.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("BCS镜像")
data class BcsImageInfo(
    @ApiModelProperty("镜像ID", required = true)
    val imageId: String,
    @ApiModelProperty("镜像名称", required = true)
    val imageName: String,
    @ApiModelProperty("镜像", required = true)
    val image: String
)