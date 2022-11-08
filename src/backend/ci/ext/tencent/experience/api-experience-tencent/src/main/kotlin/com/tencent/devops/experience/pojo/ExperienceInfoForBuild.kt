package com.tencent.devops.experience.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-体验信息-根据构建")
data class ExperienceInfoForBuild(
    @ApiModelProperty("体验名称", required = true)
    val experienceName: String,
    @ApiModelProperty("版本标题", required = true)
    val versionTitle: String,
    @ApiModelProperty("体验描述", required = true)
    val remark: String,
    @ApiModelProperty("schema跳转链接", required = true)
    val scheme: String,
    @ApiModelProperty("体验id", required = true)
    val experienceId: String
)
