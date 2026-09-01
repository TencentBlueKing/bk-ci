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
    val action: String? = null,
    @get:Schema(title = "唯一管理员组查询标识")
    val uniqueManagerGroupsQueryFlag: Boolean? = null,
    @get:Schema(title = "页码，从 1 开始；与 pageSize 同时不传则不分页，返回全部匹配组")
    val page: Int? = null,
    @get:Schema(title = "每页条数，最大 200；与 page 同时不传则不分页，返回全部匹配组")
    val pageSize: Int? = null
) {
    fun isQueryByGroupPermissions(): Boolean {
        return relatedResourceType != null
    }

    fun isQueryByGroupName(): Boolean {
        return groupName != null
    }
}
