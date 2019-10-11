package com.tencent.devops.experience.pojo

import com.tencent.devops.experience.pojo.enums.Platform
import com.tencent.devops.experience.pojo.enums.Source
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-版本信息")
data class AppExperience(
    @ApiModelProperty("版本体验ID", required = true)
    val experienceHashId: String,
    @ApiModelProperty("平台", required = true)
    val platform: Platform,
    @ApiModelProperty("来源", required = true)
    val source: Source,
    @ApiModelProperty("logo链接", required = true)
    val logoUrl: String,
    @ApiModelProperty("版本名称", required = true)
    val name: String,
    @ApiModelProperty("版本体验版本号", required = true)
    val version: String,
    @ApiModelProperty("版本体验BundleIdentifier", required = true)
    val bundleIdentifier: String
)