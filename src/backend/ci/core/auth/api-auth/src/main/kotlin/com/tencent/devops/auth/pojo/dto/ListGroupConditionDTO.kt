package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "获取用户组列表条件")
data class ListGroupConditionDTO(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "资源类型")
    val resourceType: String,
    @get:Schema(title = "资源CODE")
    val resourceCode: String,
    @get:Schema(title = "是否获取项目成员组，该字段仅在resourceType为project时生效")
    val getAllProjectMembersGroup: Boolean = false,
    @get:Schema(title = "页数")
    val page: Int,
    @get:Schema(title = "页大小")
    val pageSize: Int
)
