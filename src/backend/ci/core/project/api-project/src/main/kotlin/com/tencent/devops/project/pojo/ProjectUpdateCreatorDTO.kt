package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("修改项目创建人")
data class ProjectUpdateCreatorDTO(
    @ApiModelProperty("项目code")
    val projectCode: String,
    @ApiModelProperty("创建人")
    val creator: String
)
