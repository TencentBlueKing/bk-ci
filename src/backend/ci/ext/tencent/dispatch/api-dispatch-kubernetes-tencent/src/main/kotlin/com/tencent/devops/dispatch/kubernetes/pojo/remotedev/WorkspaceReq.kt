package com.tencent.devops.dispatch.kubernetes.pojo.remotedev

import io.swagger.annotations.ApiModelProperty

data class WorkspaceReq(
    @ApiModelProperty("工作空间ID")
    val workspaceId: Long,
    @ApiModelProperty("工作空间名称")
    val name: String,
    @ApiModelProperty("远程开发仓库地址")
    val repositoryUrl: String,
    @ApiModelProperty("仓库分支")
    val branch: String,
    @ApiModelProperty("devfile配置路径")
    val devFilePath: String?,
    @ApiModelProperty("devfile")
    val devFile: Devfile,
    @ApiModelProperty("代码库认证信息")
    val oAuthToken: String,
    @ApiModelProperty("image")
    val image: String = "",
    @ApiModelProperty("imagePullCertificate")
    val imagePullCertificate: ImagePullCertificate? = null
)
