package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import com.tencent.devops.process.pojo.template.TemplateInstanceStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板实例基本类")
data class PipelineTemplateInstanceBase(
    @get:Schema(title = "实例化请求ID", required = true)
    val baseId: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线模板ID", required = true)
    val templateId: String,
    @get:Schema(title = "流水线模板版本", required = true)
    val templateVersion: Long,
    @get:Schema(title = "是否使用模版设置", required = true)
    val useTemplateSetting: Boolean,
    @get:Schema(title = "实例化总数", required = true)
    val totalItemNum: Int,
    @get:Schema(title = "实例化成功数量", required = true)
    val successItemNum: Int,
    @get:Schema(title = "实例化失败数量", required = true)
    val failItemNum: Int,
    @get:Schema(title = "提交描述", required = true)
    val description: String?,
    @get:Schema(title = "状态", required = true)
    val status: TemplateInstanceStatus,
    @get:Schema(title = "是否开启PAC", required = true)
    val pac: Boolean,
    @get:Schema(title = "提交动作", required = true)
    val targetAction: CodeTargetAction?,
    @get:Schema(title = "实例化类型", required = true)
    val type: TemplateInstanceType,
    @get:Schema(title = "代码库hashId", required = true)
    val repoHashId: String?,
    @get:Schema(title = "代码库分支", required = true)
    val targetBranch: String?,
    @get:Schema(title = "模版引用类型", required = true)
    val templateRefType: TemplateRefType?,
    @get:Schema(title = "模版引用,分支/tag/commit", required = true)
    val templateRef: String?,
    @get:Schema(title = "合并请求链接", required = true)
    val pullRequestUrl: String?,
    val creator: String,
    val modifier: String,
    val createTime: Long,
    val updateTime: Long
)
