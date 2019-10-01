package com.tencent.devops.quality.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-用户组摘要信息")
data class GroupSummaryWithPermission(
    @ApiModelProperty("体验组HashID", required = true)
    val groupHashId: String,
    @ApiModelProperty("体验组名称", required = true)
    val name: String,
    @ApiModelProperty("内部人员", required = true)
    val innerUsersCount: Int,
    @ApiModelProperty("外部人员", required = true)
    val outerUsersCount: Int,
    @ApiModelProperty("内部人员")
    val innerUsers: Set<String>,
    @ApiModelProperty("外部人员")
    val outerUsers: String,
    @ApiModelProperty("创建者", required = true)
    val creator: String,
    @ApiModelProperty("描述", required = true)
    val remark: String,
    @ApiModelProperty("权限", required = true)
    val permissions: GroupPermission
)