package com.tencent.devops.common.archive.pojo.webhook

/**
 * bkrepo webhook 关联类型
 *
 * 关联对象Id规则：
 * - SYSTEM: associationId = ""（系统级）
 * - PROJECT: associationId = {projectId}
 * - REPO: associationId = {projectId}:{repoName}
 * - PATH: associationId = {projectId}:{repoName}:{path}，path 以 / 结尾
 */
enum class BkRepoAssociationType {
    SYSTEM,
    PROJECT,
    REPO,
    PATH;

    fun buildAssociationId(
        projectId: String? = null,
        repoName: String? = null,
        path: String? = null
    ): String {
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
            PATH -> {
                require(!projectId.isNullOrBlank()) { "projectId is required when associationType is PATH" }
                require(!repoName.isNullOrBlank()) { "repoName is required when associationType is PATH" }
                require(!path.isNullOrBlank()) { "path is required when associationType is PATH" }
                "$projectId:$repoName:$path"
            }
        }
    }
}
