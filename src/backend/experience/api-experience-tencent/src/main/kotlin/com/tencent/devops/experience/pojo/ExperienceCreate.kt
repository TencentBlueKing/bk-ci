package com.tencent.devops.experience.pojo

import com.tencent.devops.experience.pojo.enums.ArtifactoryType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-创建发布信息")
data class ExperienceCreate(
    @ApiModelProperty("版本名称", required = true)
    val name: String,
    @ApiModelProperty("文件路径", required = true)
    val path: String,
    @ApiModelProperty("版本仓库类型", required = true)
    val artifactoryType: ArtifactoryType,
    @ApiModelProperty("版本号", required = true)
    val version: String,
    @ApiModelProperty("描述", required = false)
    val remark: String?,
    @ApiModelProperty("截止日期", required = true)
    val expireDate: Long,
    @ApiModelProperty("体验组", required = true)
    val experienceGroups: Set<String>,
    @ApiModelProperty("内部名单", required = true)
    val innerUsers: Set<String>,
    @ApiModelProperty("外部名单", required = true)
    val outerUsers: String,
    @ApiModelProperty("通知类型", required = true)
    val notifyTypes: Set<NotifyType>,
    @ApiModelProperty("是否开启企业微信群", required = true)
    val enableWechatGroups: Boolean = true,
    @ApiModelProperty("企业微信群ID(逗号分隔)", required = false)
    val wechatGroups: String?
)