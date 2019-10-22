package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-文件夹大小")
data class FolderSize(
    @ApiModelProperty("文件夹大小(byte)", required = true)
    val size: Long
)