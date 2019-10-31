package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-下载统计")
data class FileDownloadCount(
    @ApiModelProperty("下载用户数", required = true)
    val downloadUsers: Long,
    @ApiModelProperty("下载次数", required = true)
    val downloadTimes: Long
)