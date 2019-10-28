package com.tencent.devops.plugin.pojo.wetest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("WeTest个人团队信息实体")
data class WetestReportResponse(
    @ApiModelProperty("项目信息")
    val projects: Map<String, Group>?,
    val ret: Int?
)

data class Group(
    val members: List<Member>?,
    val name: String?
)

data class Member(
    val name: String?,
    val id: String?
)