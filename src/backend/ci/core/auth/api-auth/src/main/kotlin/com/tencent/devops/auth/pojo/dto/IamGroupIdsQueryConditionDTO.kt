package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户组ID查询业务实体")
data class IamGroupIdsQueryConditionDTO(
    @get:Schema(title = "项目ID")
    val projectCode: String,
    @get:Schema(title = "用户组名称")
    val groupName: String? = null,
    @get:Schema(title = "组ID列表")
    val iamGroupIds: List<Int>? = null,
    @get:Schema(title = "资源类型")
    val relatedResourceType: String? = null,
    @get:Schema(title = "资源ID")
    val relatedResourceCode: String? = null,
    @get:Schema(title = "操作")
    val action: String? = null
) {
    fun isQueryByGroupPermissions(): Boolean {
        return relatedResourceType != null
    }

    fun isQueryByGroupName(): Boolean {
        return groupName != null
    }
}
