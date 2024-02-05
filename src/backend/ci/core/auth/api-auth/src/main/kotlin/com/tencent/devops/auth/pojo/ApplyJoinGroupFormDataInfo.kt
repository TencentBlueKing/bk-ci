package com.tencent.devops.auth.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("申请加入用户组itsm表单内容")
data class ApplyJoinGroupFormDataInfo(
    @ApiModelProperty("项目名称")
    val projectName: String,
    @ApiModelProperty("资源名称类型")
    val resourceTypeName: String,
    @ApiModelProperty("资源名称")
    val resourceName: String,
    @ApiModelProperty("用户组名称")
    val groupName: String,
    @ApiModelProperty("申请期限")
    val validityPeriod: String,
    @ApiModelProperty("资源跳转链接")
    val resourceRedirectUri: String? = null,
    @ApiModelProperty("用户组权限详情跳转链接")
    val groupPermissionDetailRedirectUri: String? = null
)
