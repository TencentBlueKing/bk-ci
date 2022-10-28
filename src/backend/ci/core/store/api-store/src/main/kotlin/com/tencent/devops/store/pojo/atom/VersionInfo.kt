package com.tencent.devops.store.pojo.atom

import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本信息")
data class VersionInfo(
    @ApiModelProperty("发布者", required = true)
    val publisher: String,
    @ApiModelProperty("发布类型", required = true)
    val releaseType: ReleaseTypeEnum,
    @ApiModelProperty("插件版本", required = true)
    val version: String,
    @ApiModelProperty("版本日志内容", required = true)
    @field:BkField(maxLength = 1024)
    val versionContent: String,
)
