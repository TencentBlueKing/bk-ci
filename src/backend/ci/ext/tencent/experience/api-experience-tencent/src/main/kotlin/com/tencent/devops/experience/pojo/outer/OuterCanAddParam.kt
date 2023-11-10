package com.tencent.devops.experience.pojo.outer

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("是否可以添加外部用户--请求")
data class OuterCanAddParam(
    @ApiModelProperty("用户列表,用英文,分隔", required = true)
    val userIds: String
)
