package com.tencent.devops.common.archive.pojo.webhook

/**
 * bkrepo webhook 关联类型
 *
 * 关联对象Id规则：
 * - SYSTEM: associationId = ""（系统级）
 * - PROJECT: associationId = {projectId}
 * - REPO: associationId = {projectId}:{repoName}
 */
enum class BkRepoAssociationType {
    SYSTEM,
    PROJECT,
    REPO;

    fun buildAssociationId(projectId: String? = null, repoName: String? = null): String {
        return when (this) {
            SYSTEM -> ""
            PROJECT -> {
                require(!projectId.isNullOrBlank()) { "projectId is required when associationType is PROJECT" }
                projectId
            }
            REPO -> {
                require(!projectId.isNullOrBlank()) { "projectId is required when associationType is REPO" }
                require(!repoName.isNullOrBlank()) { "repoName is required when associationType is REPO" }
                "$projectId:$repoName"
            }
        }
    }
}
