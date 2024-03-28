package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.auth.pojo.ManagerRoleGroupInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户组信息返回实体")
data class ManagerRoleGroupVO(
    @get:Schema(title = "用户组数量")
    val count: Int,
    @get:Schema(title = "用户组信息")
    val results: List<ManagerRoleGroupInfo>
)
