package com.tencent.devops.experience.pojo.download

import com.tencent.devops.experience.pojo.ExperienceChangeLog
import com.tencent.devops.experience.pojo.enums.Platform
import io.swagger.annotations.ApiModelProperty

data class CheckVersionVO(
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
    @ApiModelProperty("包名称", required = true)
    val packageName: String,
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
    val changeLog: List<ExperienceChangeLog>,
    @ApiModelProperty("体验名称", required = true)
    val experienceName: String,
    @ApiModelProperty("版本标题", required = true)
    val versionTitle: String,
    @ApiModelProperty("产品类别", required = true)
    val categoryId: Int,
    @ApiModelProperty("产品负责人", required = true)
    val productOwner: List<String>,
    @ApiModelProperty("创建时间", required = true)
    val createDate: Long,
    @ApiModelProperty("体验截至时间", required = true)
    val endDate: Long,
    @ApiModelProperty("是否为公开体验", required = true)
    val publicExperience: Boolean,
    @ApiModelProperty("描述", required = true)
    val remark: String
)
