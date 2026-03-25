package com.tencent.devops.common.pipeline.pojo

import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 一键创建流水线并保存草稿的请求参数
 */
data class CreatePipelineAndSaveDraftRequest(
    @get:Schema(title = "模板ID", required = true)
    var templateId: String,
    @get:Schema(title = "模板版本号（为空时默认最新）", required = true)
    var templateVersion: Long?,
    @get:Schema(title = "流水线名称", required = true)
    val pipelineName: String,
    @get:Schema(title = "是否使用通知配置", required = false)
    var useSubscriptionSettings: Boolean?,
    @get:Schema(title = "是否使用标签配置", required = false)
    var useLabelSettings: Boolean?,
    @get:Schema(title = "是否使用并发组配置", required = false)
    var useConcurrencyGroup: Boolean?,
    @get:Schema(title = "创建实例的模式", required = false)
    var instanceType: String? = PipelineInstanceTypeEnum.FREEDOM.type,
    @get:Schema(title = "是否为空模板", required = false)
    var emptyTemplate: Boolean? = false,
    @get:Schema(title = "静态流水线组", required = false)
    var staticViews: List<String> = emptyList(),
    @get:Schema(title = "是否继承项目流水线语言风格", required = false)
    var inheritedDialect: Boolean? = true,
    @get:Schema(title = "流水线语言风格", required = false)
    var pipelineDialect: String? = null,
    @get:Schema(title = "流水线标签", required = false)
    val labels: List<String> = emptyList(),
    @get:Schema(title = "流水线模型", required = true)
    val modelAndSetting: PipelineModelAndSetting?,
    @get:Schema(title = "流水线YAML编排（不为空时以YAML为准）", required = false)
    val yaml: String?,
    @get:Schema(title = "存储格式", required = false)
    val storageType: PipelineStorageType? = PipelineStorageType.MODEL,
    @get:Schema(title = "版本变更说明", required = false)
    val description: String? = null
)