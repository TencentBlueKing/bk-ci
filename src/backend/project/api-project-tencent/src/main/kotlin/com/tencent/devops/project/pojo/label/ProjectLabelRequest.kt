package com.tencent.devops.project.pojo.label
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目标签请求实体")
data class ProjectLabelRequest(
    @ApiModelProperty("标签名称")
    val labelName: String
)