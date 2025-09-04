package com.tencent.devops.remotedev.pojo.gitproxy

import io.swagger.v3.oas.annotations.media.Schema

data class CreateTGitProjectInfo(
    @get:Schema(title = "蓝盾项目ID")
    val projectId: String,
    @get:Schema(title = "工蜂仓库名称")
    val name: String,
    @get:Schema(title = "命名空间ID，非必填")
    val namespaceId: Long?,
    @get:Schema(title = "是否是svn项目")
    val svnProject: Boolean,
    @get:Schema(title = "凭据 ID")
    val credId: String?
)
