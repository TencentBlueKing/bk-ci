package com.tencent.devops.store.pojo.template

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("安装模板到项目返回报文")
data class InstallProjectTemplateDTO(
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("所属项目模板ID")
    val templateId: String,
    @ApiModelProperty("所属项目模板版本")
    val version: Long
)
