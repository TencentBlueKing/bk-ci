package com.tencent.devops.support.model.app

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("App版本日志请求体")
data class AppVersionRequest(
    @ApiModelProperty("版本号")
    val versionId: String = "",
    @ApiModelProperty("发布日志")
    val releaseDate: Long = 0,
    @ApiModelProperty("发布内容")
    val releaseContent: String = "",
    @ApiModelProperty("渠道类型（1:\"安卓\", 2:\"IOS\"）")
    val channelType: Byte = 1
)