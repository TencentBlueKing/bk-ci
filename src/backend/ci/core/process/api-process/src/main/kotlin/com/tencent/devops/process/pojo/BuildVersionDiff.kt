package com.tencent.devops.process.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模型-构建版本差异")
data class BuildVersionDiff(
    @get:Schema(title = "上一次版本", required = true)
    val prevVersion: Int? = null,
    @get:Schema(title = "上一次版本名称", required = true)
    val prevVersionName: String? = null,
    @get:Schema(title = "当前版本", required = true)
    val currVersion: Int,
    @get:Schema(title = "当前版本名称", required = true)
    val currVersionName: String?,
    @get:Schema(title = "引用资源版本差异", required = true)
    val buildVersionDiffs: List<BuildVersionDiffInfo>
)
