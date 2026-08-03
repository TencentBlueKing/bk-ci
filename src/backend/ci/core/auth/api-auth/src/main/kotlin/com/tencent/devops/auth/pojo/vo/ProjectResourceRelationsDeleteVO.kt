package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目资源关系删除结果")
data class ProjectResourceRelationsDeleteVO(
    @get:Schema(title = "项目Code", required = true)
    val projectCode: String,
    @get:Schema(title = "资源类型", required = true)
    val resourceType: String,
    @get:Schema(title = "是否预演模式", required = true)
    val dryRun: Boolean,
    @get:Schema(title = "是否显式确认删除", required = true)
    val confirm: Boolean,
    @get:Schema(title = "是否后台异步执行", required = true)
    val async: Boolean,
    @get:Schema(title = "是否已提交异步任务", required = true)
    val submitted: Boolean,
    @get:Schema(title = "是否已实际执行删除", required = true)
    val executed: Boolean,
    @get:Schema(title = "匹配到的资源总数", required = true)
    val totalCount: Int,
    @get:Schema(title = "实际删除数量", required = true)
    val deletedCount: Int,
    @get:Schema(title = "预览列表上限", required = true)
    val previewLimit: Int,
    @get:Schema(title = "前N个资源Code预览", required = true)
    val previewResourceCodes: List<String>
)
