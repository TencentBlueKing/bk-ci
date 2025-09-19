package com.tencent.devops.process.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模型-构建版本引用资源版本差异")
data class BuildResourceVersionDiff(
    @get:Schema(title = "资源ID", required = true)
    val resourceId: String,
    @get:Schema(title = "资源名称", required = true)
    val resourceName: String,
    @get:Schema(title = "资源版本名称", required = true)
    val resourceVersionName: String,
    @get:Schema(title = "上一次版本", required = true)
    val lastVersion: Int,
    @get:Schema(title = "上一次版本名称", required = true)
    val lastVersionName: String,
    @get:Schema(title = "当前版本", required = true)
    val currVersion: Int,
    @get:Schema(title = "当前版本名称", required = true)
    val currVersionName: String,
)
