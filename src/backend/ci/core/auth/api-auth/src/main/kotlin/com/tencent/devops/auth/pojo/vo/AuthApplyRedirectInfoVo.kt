package com.tencent.devops.auth.pojo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("弹框跳转信息")
data class AuthApplyRedirectInfoVo(
    @ApiModelProperty("是否开启权限")
    val auth: Boolean,
    @ApiModelProperty("资源类型名称")
    val resourceTypeName: String,
    @ApiModelProperty("资源实例名称")
    val resourceName: String,
    @ApiModelProperty("动作名称")
    val actionName: String? = null,
    @ApiModelProperty("用户组信息列表")
    val groupInfoList: List<AuthRedirectGroupInfoVo>
)
