package com.tencent.devops.project.api.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目OBS产品相关信息")
data class ProjectProductInfo(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "项目名称")
    val projectName: String,
    @get:Schema(title = "运营产品ID")
    val productId: Int,
    @get:Schema(title = "运营产品名称")
    val productName: String,
    @get:Schema(title = "管理员")
    val managers: List<String>
)
