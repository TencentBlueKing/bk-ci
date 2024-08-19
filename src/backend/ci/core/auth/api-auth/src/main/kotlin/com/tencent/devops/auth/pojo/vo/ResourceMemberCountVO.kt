package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "资源成员数量")
data class ResourceMemberCountVO(
    @get:Schema(title = "用户组人数")
    val userCount: Int,
    @get:Schema(title = "用户组部门数")
    val departmentCount: Int
)
