package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-多路径")
data class PathList(
    @ApiModelProperty("路径列表", required = true)
    val paths: List<String>
)