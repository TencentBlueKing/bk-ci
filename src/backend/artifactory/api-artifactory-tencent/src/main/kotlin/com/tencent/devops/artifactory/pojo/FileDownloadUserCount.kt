package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-文件下载统计")
data class FileDownloadUserCount(
    @ApiModelProperty("下载用户", required = true)
    val userId: String,
    @ApiModelProperty("下载次数", required = true)
    val count: Int,
    @ApiModelProperty("最后下载时间(秒)", required = true)
    val updateTime: Long
)