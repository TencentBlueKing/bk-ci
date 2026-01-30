package com.tencent.devops.common.webhook.pojo.code.git

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

/**
 * 项目相关事件数据模型
 */
data class GitGroupEvent(
    @JsonProperty("operation_kind")
    val operationKind: String, // 操作类型（如 "project_dereference"）
    val user: GitUser, // 操作用户信息
    val group: GitGroup, // 项目所属分组信息
    @JsonProperty("object_attributes")
    val objectAttributes: GitProjectAttributes // 项目核心属性
) : GitEvent() {
    companion object {
        const val classType = "project"
    }
}

/**
 * 项目所属分组信息模型
 */
data class GitGroup(
    @JsonProperty("group_id")
    val groupId: Int, // 分组ID
    val name: String, // 分组名称
    val path: String, // 分组路径
    @JsonProperty("full_path")
    val fullPath: String, // 分组完整路径
    val homepage: String // 分组主页URL
)

/**
 * 项目核心属性模型
 */
data class GitProjectAttributes(
    @JsonProperty("project_id")
    val projectId: Int, // 项目ID
    
    @JsonProperty("visibility_level")
    val visibilityLevel: Int, // 可见性级别（10 通常表示私有）
    
    @JsonProperty("project_type")
    val projectType: String, // 项目类型（如 "GIT"）
    
    @JsonProperty("created_at")
    val createdAt: OffsetDateTime, // 创建时间（带时区）
    
    @JsonProperty("updated_at")
    val updatedAt: OffsetDateTime, // 更新时间（带时区）
    
    val name: String, // 项目名称
    
    val path: String, // 项目路径
    
    @JsonProperty("path_with_namespace")
    val pathWithNamespace: String, // 项目带命名空间的完整路径

    @JsonProperty("old_path_with_namespace")
    val oldPathWithNamespace: String? // 原始地址
)
