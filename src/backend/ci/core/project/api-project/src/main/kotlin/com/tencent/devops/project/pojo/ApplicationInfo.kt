package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目-申请加入项目实体类")
data class ApplicationInfo(
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("过期时间")
    val expireTime: String,
    @ApiModelProperty("申请理由")
    val reason: String
)
