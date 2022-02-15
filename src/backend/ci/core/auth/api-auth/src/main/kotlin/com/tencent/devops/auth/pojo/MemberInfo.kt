package com.tencent.devops.auth.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("成员信息")
data class MemberInfo(
    @ApiModelProperty("成员id")
    val id: String,
    @ApiModelProperty("成员名称")
    val name: String,
    @ApiModelProperty("成员类别")
    val type: String
)
