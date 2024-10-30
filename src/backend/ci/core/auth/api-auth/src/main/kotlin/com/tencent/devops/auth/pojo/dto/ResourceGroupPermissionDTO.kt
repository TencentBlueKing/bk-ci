package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.Objects

@Schema(title = "资源组权限详情")
data class ResourceGroupPermissionDTO(
    @get:Schema(title = "id")
    val id: Long? = null,
    @get:Schema(title = "项目code")
    val projectCode: String,
    @get:Schema(title = "用户组关联的资源类型")
    val resourceType: String,
    @get:Schema(title = "用户组关联的资源code")
    val resourceCode: String,
    @get:Schema(title = "用户组关联的iam资源code")
    val iamResourceCode: String,
    @get:Schema(title = "用户组标识")
    val groupCode: String,
    @get:Schema(title = "用户组ID")
    val iamGroupId: Int,
    @get:Schema(title = "操作")
    val action: String,
    @get:Schema(title = "操作关联的资源类型")
    val actionRelatedResourceType: String,
    @get:Schema(title = "组权限关联的资源类型")
    val relatedResourceType: String,
    @get:Schema(title = "组权限关联的资源code")
    val relatedResourceCode: String,
    @get:Schema(title = "组权限关联的iam资源code")
    val relatedIamResourceCode: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val otherObj = other as ResourceGroupPermissionDTO
        return projectCode == otherObj.projectCode &&
            iamGroupId == otherObj.iamGroupId &&
            relatedResourceType == otherObj.relatedResourceType &&
            relatedResourceCode == otherObj.relatedResourceCode &&
            action == otherObj.action
    }

    override fun hashCode(): Int {
        return Objects.hash(projectCode, iamGroupId, relatedResourceType, relatedResourceCode, action)
    }
}
