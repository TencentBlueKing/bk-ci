package com.tencent.devops.dispatch.kubernetes.pojo.remotedev

import io.swagger.v3.oas.annotations.media.Schema

data class WorkspaceReq(
    @get:Schema(title = "工作空间ID")
    val workspaceId: Long,
    @get:Schema(title = "工作空间名称")
    val name: String,
    @get:Schema(title = "远程开发仓库地址")
    val repositoryUrl: String,
    @get:Schema(title = "仓库分支")
    val branch: String,
    @get:Schema(title = "devfile配置路径")
    val devFilePath: String?,
    @get:Schema(title = "devfile")
    val devFile: Devfile,
    @get:Schema(title = "代码库认证信息")
    val oAuthToken: String,
    @get:Schema(title = "image")
    val image: String = "",
    @get:Schema(title = "imagePullCertificate")
    val imagePullCertificate: ImagePullCertificate? = null
)
