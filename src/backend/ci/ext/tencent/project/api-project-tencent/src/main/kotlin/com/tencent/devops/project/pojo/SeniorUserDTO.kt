package com.tencent.devops.project.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("高级用户实体")
data class SeniorUserDTO(
    @ApiModelProperty("用户ID")
    val userId: String,
    @ApiModelProperty("用户名称")
    val name: String,
    @ApiModelProperty("bg名称")
    val bgName: String
)
