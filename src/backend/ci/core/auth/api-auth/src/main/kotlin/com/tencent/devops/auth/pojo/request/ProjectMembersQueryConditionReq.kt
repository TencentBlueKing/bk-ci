package com.tencent.devops.auth.pojo.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目成员查询业务处理请求体")
data class ProjectMembersQueryConditionReq(
    @get:Schema(title = "项目ID")
    val projectCode: String,
    @get:Schema(title = "成员类型")
    val memberType: String?,
    @get:Schema(title = "用户名称")
    val userName: String?,
    @get:Schema(title = "部门名称")
    val deptName: String?,
    @get:Schema(title = "用户组名称")
    val groupName: String?,
    @get:Schema(title = "最小过期时间")
    val minExpiredAt: Long?,
    @get:Schema(title = "最大过期时间")
    val maxExpiredAt: Long?,
    @get:Schema(title = "离职标识")
    val departedFlag: Boolean? = false,
    @get:Schema(title = "资源类型")
    val relatedResourceType: String? = null,
    @get:Schema(title = "资源ID")
    val relatedResourceCode: String? = null,
    @get:Schema(title = "操作")
    val action: String? = null,
    @get:Schema(title = "第几页")
    val page: Int,
    @get:Schema(title = "页数")
    val pageSize: Int
) {
    // 当查询到权限相关信息时，如组名称，过期时间，操作，资源类型时，走复杂查询逻辑
    fun isComplexQuery(): Boolean {
        return groupName != null || minExpiredAt != null || maxExpiredAt != null ||
            relatedResourceType != null || relatedResourceCode != null || action != null
    }

    // 是否需要进行查询用户组，当涉及到查询 组名称，组权限相关，需要进行查询
    fun isNeedToQueryIamGroups(): Boolean {
        return groupName != null || relatedResourceType != null ||
            relatedResourceCode != null || action != null
    }
}
