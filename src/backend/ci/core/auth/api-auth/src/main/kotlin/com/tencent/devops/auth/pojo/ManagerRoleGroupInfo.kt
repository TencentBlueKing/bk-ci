package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户组详细信息")
data class ManagerRoleGroupInfo(
    @get:Schema(title = "用户组id")
    val id: Int,
    @get:Schema(title = "名称")
    val name: String,
    @get:Schema(title = "描述")
    val description: String,
    @get:Schema(title = "是否是只读用户组")
    val readonly: Boolean,
    @get:Schema(title = "用户组成员user数量")
    val userCount: Int,
    @get:Schema(title = "用户组成员department数量")
    val departmentCount: Int,
    @get:Schema(title = "是否已经加入用户组")
    val joined: Boolean,
    @get:Schema(title = "用户组关联的资源类型")
    val resourceType: String,
    @get:Schema(title = "用户组关联的资源类型名称")
    val resourceTypeName: String,
    @get:Schema(title = "用户组关联的资源实例名称")
    val resourceName: String,
    @get:Schema(title = "用户组关联的资源实例code")
    val resourceCode: String
)
