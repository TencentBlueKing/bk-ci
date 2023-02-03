package com.tencent.devops.auth.pojo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("弹框跳转信息")
data class AuthApplyJumpInfoVo(
    @ApiModelProperty("是否开启权限")
    val auth: Boolean,
    @ApiModelProperty("用户组信息列表")
    val groupInfoList: ArrayList<AuthJumpGroupInfoVo>
)
