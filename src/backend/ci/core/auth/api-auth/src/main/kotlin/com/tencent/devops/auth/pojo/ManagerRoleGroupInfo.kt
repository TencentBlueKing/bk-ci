package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "用户组详细信息")
data class ManagerRoleGroupInfo(
    @Schema(description = "用户组id")
    val id: Int,
    @Schema(description = "名称")
    val name: String,
    @Schema(description = "描述")
    val description: String,
    @Schema(description = "是否是只读用户组")
    val readonly: Boolean,
    @Schema(description = "用户组成员user数量")
    val userCount: Int,
    @Schema(description = "用户组成员department数量")
    val departmentCount: Int,
    @Schema(description = "是否已经加入用户组")
    val joined: Boolean,
    @Schema(description = "用户组关联的资源类型")
    val resourceType: String,
    @Schema(description = "用户组关联的资源类型名称")
    val resourceTypeName: String,
    @Schema(description = "用户组关联的资源实例名称")
    val resourceName: String,
    @Schema(description = "用户组关联的资源实例code")
    val resourceCode: String
)
