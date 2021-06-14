package com.tencent.devops.auth.pojo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("用户组列表返回")
data class GroupInfoVo (
    @ApiModelProperty("用户组ID")
    val id : Int,
    @ApiModelProperty("用户组名称")
    val name: String
)
