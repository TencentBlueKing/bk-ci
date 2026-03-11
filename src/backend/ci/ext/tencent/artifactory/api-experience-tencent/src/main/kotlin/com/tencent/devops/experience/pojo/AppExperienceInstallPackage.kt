package com.tencent.devops.experience.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "体验--安装包")
data class AppExperienceInstallPackage(
    @get:Schema(title = "名称", required = true)
    val name: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "路径", required = true)
    val path: String,
    @get:Schema(title = "仓库类型", required = true)
    val artifactoryType: String,
    @get:Schema(title = "是否有跳转构件详情的权限", required = true)
    val detailPermission: Boolean,
    @get:Schema(title = "大小", required = true)
    val size: Long
)
