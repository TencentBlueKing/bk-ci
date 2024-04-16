package com.tencent.devops.remotedev.pojo.gitproxy

import io.swagger.v3.oas.annotations.media.Schema

data class CreateTGitProjectInfo(
    val name: String,
    val namespaceId: Long?,
    @get:Schema(title = "是否是svn项目")
    val svnProject: Boolean
)
