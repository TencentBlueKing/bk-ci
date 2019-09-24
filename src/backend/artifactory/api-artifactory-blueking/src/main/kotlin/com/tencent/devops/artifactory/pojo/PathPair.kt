package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-路径对")
data class PathPair(
    @ApiModelProperty("源路径", required = true)
    val srcPath: String,
    @ApiModelProperty("目标路径", required = true)
    val destPath: String
)