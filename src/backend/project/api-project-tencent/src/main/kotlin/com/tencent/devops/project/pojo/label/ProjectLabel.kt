package com.tencent.devops.project.pojo.label
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目标签")
data class ProjectLabel(
    @ApiModelProperty("主键ID")
    val id: String,
    @ApiModelProperty("标签名称")
    val labelName: String
)