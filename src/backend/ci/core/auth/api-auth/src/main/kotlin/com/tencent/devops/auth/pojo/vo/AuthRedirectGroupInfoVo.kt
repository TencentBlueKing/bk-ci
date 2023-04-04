package com.tencent.devops.auth.pojo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("权限申请跳转-用户组信息")
data class AuthRedirectGroupInfoVo(
    @ApiModelProperty("跳转URL")
    val url: String,
    @ApiModelProperty("用户组名")
    val groupName: String? = null
)
