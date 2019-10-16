package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-文件摘要")
data class FileChecksums(
    @ApiModelProperty("sha256", required = true)
    val sha256: String?,
    @ApiModelProperty("sha1", required = true)
    val sha1: String,
    @ApiModelProperty("md5", required = true)
    val md5: String
)