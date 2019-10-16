package com.tencent.devops.experience.pojo

import com.tencent.devops.experience.pojo.enums.Platform
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-版本详情")
data class AppExperienceDetail(
    @ApiModelProperty("版本体验ID", required = true)
    val experienceHashId: String,
    @ApiModelProperty("文件大小(byte)", required = true)
    val size: Long,
    @ApiModelProperty("logo链接", required = true)
    val logoUrl: String,
    @ApiModelProperty("分享链接", required = true)
    val shareUrl: String,
    @ApiModelProperty("版本名称", required = true)
    val name: String,
    @ApiModelProperty("平台", required = true)
    val platform: Platform,
    @ApiModelProperty("版本体验版本号", required = true)
    val version: String,
    @ApiModelProperty("是否已过期", required = true)
    val expired: Boolean,
    @ApiModelProperty("是否可体验", required = true)
    val canExperience: Boolean,
    @ApiModelProperty("是否在线", required = true)
    val online: Boolean,
    @ApiModelProperty("更新日志", required = true)
    val changeLog: List<ExperienceChangeLog>
)