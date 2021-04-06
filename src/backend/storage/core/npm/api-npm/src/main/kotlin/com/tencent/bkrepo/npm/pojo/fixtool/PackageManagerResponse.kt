package com.tencent.bkrepo.npm.pojo.fixtool

/**
 * 包管理包装数据返回
 */
data class PackageManagerResponse(
    val projectId: String,
    val repoName: String,
    val totalCount: Long,
    val successCount: Long,
    val failedCount: Long,
    val failedSet: MutableSet<FailPackageDetail> = mutableSetOf()
) {
    companion object {
        fun emptyResponse(projectId: String, repoName: String) =
            PackageManagerResponse(projectId, repoName, 0, 0, 0)
    }
}

data class FailPackageDetail(
    val name: String,
    val failedVersionSet: MutableSet<FailVersionDetail>
)

data class FailVersionDetail(
    val version: String,
    val reason: String
)
