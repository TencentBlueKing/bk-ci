package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModelProperty

data class ProjectCreateUserDTO (
    @ApiModelProperty("目标项目Id")
    val projectId: String,
    @ApiModelProperty("目标用户id")
    val userId: String
)