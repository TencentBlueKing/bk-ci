package com.tencent.devops.dispatch.kubernetes.pojo.remotedev

import io.swagger.v3.oas.annotations.media.Schema

data class WorkspaceReq(
    @Schema(description = "工作空间ID")
    val workspaceId: Long,
    @Schema(description = "工作空间名称")
    val name: String,
    @Schema(description = "远程开发仓库地址")
    val repositoryUrl: String,
    @Schema(description = "仓库分支")
    val branch: String,
    @Schema(description = "devfile配置路径")
    val devFilePath: String?,
    @Schema(description = "devfile")
    val devFile: Devfile,
    @Schema(description = "代码库认证信息")
    val oAuthToken: String,
    @Schema(description = "image")
    val image: String = "",
    @Schema(description = "imagePullCertificate")
    val imagePullCertificate: ImagePullCertificate? = null
)
