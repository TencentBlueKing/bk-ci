package com.tencent.devops.project.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("蓝盾-服务简要信息")
class ExtServiceEntity (
    @ApiModelProperty("主键ID")
    val id: String,
    @ApiModelProperty("名称")
    val name: String
)