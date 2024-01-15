package com.tencent.devops.experience.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "体验--安装包")
data class AppExperienceInstallPackage(
    @Schema(description = "名称", required = true)
    val name: String,
    @Schema(description = "项目ID", required = true)
    val projectId: String,
    @Schema(description = "路径", required = true)
    val path: String,
    @Schema(description = "仓库类型", required = true)
    val artifactoryType: String,
    @Schema(description = "是否有跳转构件详情的权限", required = true)
    val detailPermission: Boolean,
    @Schema(description = "大小", required = true)
    val size: Long
)
