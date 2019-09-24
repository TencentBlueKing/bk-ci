package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-组合路径")
data class CombinationPath(
    @ApiModelProperty("原路径列表", required = true)
    val srcPaths: List<String>,
    @ApiModelProperty("目标路径", required = true)
    val destPath: String
)