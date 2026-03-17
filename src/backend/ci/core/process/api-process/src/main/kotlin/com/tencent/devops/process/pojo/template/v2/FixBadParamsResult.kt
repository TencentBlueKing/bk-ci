package com.tencent.devops.process.pojo.template.v2

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "修复模板错误参数-汇总结果")
data class FixBadParamsResult(
    @get:Schema(title = "是否仅预览")
    val dryRun: Boolean,
    @get:Schema(title = "有问题的模板列表")
    val templates: List<FixBadParamsTemplateDetail>,
    @get:Schema(title = "有问题的模板总数")
    val affectedTemplateCount: Int,
    @get:Schema(title = "有问题的版本总数")
    val affectedVersionCount: Int
)

@Schema(title = "修复模板错误参数-模板详情")
data class FixBadParamsTemplateDetail(
    @get:Schema(title = "模板ID")
    val templateId: String,
    @get:Schema(title = "模板名称")
    val templateName: String,
    @get:Schema(title = "有问题的版本列表")
    val versions: List<FixBadParamsVersionDetail>
)

@Schema(title = "修复模板错误参数-版本详情")
data class FixBadParamsVersionDetail(
    @get:Schema(title = "版本号")
    val version: Long,
    @get:Schema(title = "版本名称")
    val versionName: String?,
    @get:Schema(title = "有问题的参数ID列表")
    val badParamIds: List<String>
)
