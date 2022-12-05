package com.tencent.bkrepo.repository.pojo.software

import io.swagger.annotations.ApiModel

@ApiModel("软件源包搜索结果总览")
data class ProjectPackageOverview(
    val projectId: String,
    var repos: MutableSet<RepoPackageOverview>,
    var sum: Long
) {
    data class RepoPackageOverview(
        val repoName: String,
        val packages: Long
    )

    override fun equals(other: Any?): Boolean = (this === other) ||
        ((other is ProjectPackageOverview) && this.projectId == other.projectId)

    override fun hashCode(): Int = projectId.hashCode()
}
