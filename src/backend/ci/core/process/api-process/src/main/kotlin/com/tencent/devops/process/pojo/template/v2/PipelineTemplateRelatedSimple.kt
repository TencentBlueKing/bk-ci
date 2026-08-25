package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.process.pojo.template.TemplatePipelineStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模板与流水线关联简单实体")
data class PipelineTemplateRelatedSimple(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "模板Id", required = true)
    val templateId: String,
    @get:Schema(title = "模板版本号", required = true)
    val version: Long,
    @get:Schema(title = "模板版本名称", required = true)
    val versionName: String,
    @get:Schema(title = "模板最新版本", required = true)
    val releasedVersion: Long,
    @get:Schema(title = "流水线Id", required = true)
    val pipelineId: String,
    @get:Schema(title = "流水线Id", required = true)
    val pipelineName: String,
    @get:Schema(title = "流水线版本", required = true)
    val pipelineVersion: Int,
    @get:Schema(title = "实例化类型", required = false)
    val instanceType: PipelineInstanceTypeEnum,
    @get:Schema(title = "实例化错误信息", required = true)
    val instanceErrorInfo: String?,
    @get:Schema(title = "创建时间", required = true)
    val createdTime: Long,
    @get:Schema(title = "更新时间", required = true)
    val updatedTime: Long,
    @get:Schema(title = "创建人", required = true)
    val creator: String,
    @get:Schema(title = "更新人", required = true)
    val updater: String,
    @get:Schema(title = "状态")
    val status: TemplatePipelineStatus?,
    @get:Schema(title = "合并请求链接")
    val pullRequestUrl: String? = null
)
