package com.tencent.devops.common.pipeline.pojo.coverity

import com.tencent.devops.common.api.enums.RepositoryConfig

/**
 * deng
 * 26/01/2018
 */
data class CoverityConfig(
    val name: String,
    val cnName: String,
    val projectType: CoverityProjectType,
    val tools: List<String>,
    var asynchronous: Boolean, // 是否同步，默认是同步
    val filterTools: List<String>,
    val repos: List<RepoItem>,
    val scanCodePath: String,
    val scmType: String,
    val certType: String,
    val timeOut: Long = 4 * 3600 // 4小时
) {
    data class RepoItem(
        val repositoryConfig: RepositoryConfig,
        val type: String,
        val relPath: String = "", // 代码路径
        val relativePath: String = "", // 代码相对路径
        var url: String = "",
        var authType: String = "",
        var repoHashId: String = ""
    )
}
