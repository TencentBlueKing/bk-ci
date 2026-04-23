package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "推荐候选用户组")
data class CandidateGroupVO(
    @get:Schema(title = "用户组名称", required = true)
    val groupName: String,
    @get:Schema(title = "管理层级", required = true)
    val managementLevel: String,
    @get:Schema(title = "管理范围", required = true)
    val managementScope: String,
    @get:Schema(title = "拥有的权限列表", required = true)
    val permissions: List<String>,
    @get:Schema(title = "用户组关联ID", required = true)
    val relationId: Int,
    @get:Schema(title = "推荐/警告标签")
    val tags: List<PermissionTagVO> = emptyList(),
    @get:Schema(title = "目标用户是否已是成员")
    val alreadyMember: Boolean = false
)
