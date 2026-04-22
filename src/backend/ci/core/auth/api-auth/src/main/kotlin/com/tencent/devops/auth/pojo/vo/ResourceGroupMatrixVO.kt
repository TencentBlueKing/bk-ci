package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "资源权限矩阵中的用户组信息")
data class ResourceGroupMatrixVO(
    @get:Schema(title = "用户组名称", required = true)
    val groupName: String,
    @get:Schema(title = "管理层级（项目/流水线组/流水线）", required = true)
    val managementLevel: String,
    @get:Schema(title = "管理范围", required = true)
    val managementScope: String,
    @get:Schema(title = "拥有的权限列表", required = true)
    val permissions: List<String>,
    @get:Schema(title = "成员人数", required = true)
    val userCount: Int,
    @get:Schema(title = "成员列表")
    val users: List<String> = emptyList(),
    @get:Schema(title = "关联组织数量")
    val orgCount: Int = 0,
    @get:Schema(title = "关联组织列表")
    val orgs: List<String> = emptyList(),
    @get:Schema(title = "用户组关联ID", required = true)
    val relationId: Int
)
