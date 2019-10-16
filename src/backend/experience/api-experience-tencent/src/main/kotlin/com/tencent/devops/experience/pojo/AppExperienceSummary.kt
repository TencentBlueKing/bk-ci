package com.tencent.devops.experience.pojo

import com.tencent.devops.experience.pojo.enums.Platform
import com.tencent.devops.experience.pojo.enums.Source
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-发布摘要")
data class AppExperienceSummary(
    @ApiModelProperty("发布HashId", required = true)
    val experienceHashId: String,
    @ApiModelProperty("版本名称", required = true)
    val name: String,
    @ApiModelProperty("平台", required = true)
    val platform: Platform,
    @ApiModelProperty("版本号", required = true)
    val version: String,
    @ApiModelProperty("描述", required = false)
    val remark: String?,
    @ApiModelProperty("截止日期", required = true)
    val expireDate: Long,
    @ApiModelProperty("来源", required = true)
    val source: Source,
    @ApiModelProperty("logo链接", required = true)
    val logoUrl: String,
    @ApiModelProperty("创建者", required = true)
    val creator: String,
    @ApiModelProperty("是否已过期", required = true)
    val expired: Boolean,
    @ApiModelProperty("是否可体验", required = true)
    val canExperience: Boolean,
    @ApiModelProperty("是否在线", required = true)
    val online: Boolean
)