package com.tencent.devops.store.api.image.op.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "第三方镜像市场初始化参数")
data class ImageInitRequest(
    @Schema(description = "所属项目ID", required = true)
    val projectCode: String,
    @Schema(description = "所属项目描述", required = true)
    val projectDesc: String?,
    @Schema(description = "拥有者用户ID", required = true)
    val userId: String,
    @Schema(description = "市场镜像ID", required = true)
    val imageCode: String,
    @Schema(description = "镜像鉴权accessToken", required = false)
    val accessToken: String?,
    @Schema(description = "凭据ID", required = false)
    val ticketId: String?,
    @Schema(description = "镜像简介", required = false)
    val summary: String?,
    @Schema(description = "镜像描述", required = false)
    val description: String?,
    @Schema(description = "logo地址", required = false)
    val logoUrl: String?,
    @Schema(description = "自定义icon图标字符串", required = false)
    val iconData: String?,
    @Schema(description = "镜像仓库地址", required = false)
    val imageRepoUrl: String?,
    @Schema(description = "镜像仓库名", required = false)
    val imageRepoName: String?,
    @Schema(description = "镜像标签", required = false)
    val imageTag: String?,
    @Schema(description = "镜像DockerFile类型", required = false)
    val dockerFileType: String?,
    @Schema(description = "镜像DockerFile内容", required = false)
    val dockerFileContent: String?,
    @Schema(description = "镜像版本", required = false)
    val versionContent: String?
)
