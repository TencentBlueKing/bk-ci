package com.tencent.devops.dispatch.kubernetes.pojo.remotedev

import io.swagger.v3.oas.annotations.media.Schema

data class WorkspaceReq(
    @Schema(title = "工作空间ID")
    val workspaceId: Long,
    @Schema(title = "工作空间名称")
    val name: String,
    @Schema(title = "远程开发仓库地址")
    val repositoryUrl: String,
    @Schema(title = "仓库分支")
    val branch: String,
    @Schema(title = "devfile配置路径")
    val devFilePath: String?,
    @Schema(title = "devfile")
    val devFile: Devfile,
    @Schema(title = "代码库认证信息")
    val oAuthToken: String,
    @Schema(title = "image")
    val image: String = "",
    @Schema(title = "imagePullCertificate")
    val imagePullCertificate: ImagePullCertificate? = null
)
