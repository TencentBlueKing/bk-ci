package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.auth.pojo.MemberInfo
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("项目成员列表返回")
data class ProjectMembersVO(
    @ApiModelProperty("数量")
    val count: Int,
    @ApiModelProperty("成员信息列表")
    val results: Set<MemberInfo>
)
