package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模版流水线关联版本查询条件")
data class PTemplatePipelineVersionCommonCondition(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线Id", required = false)
    val pipelineId: String? = null,
    @get:Schema(title = "流水线版本号", required = false)
    val pipelineVersion: Int? = null,
    @get:Schema(title = "流水线版本名称", required = false)
    val pipelineVersionName: String? = null,
    @get:Schema(title = "实例化类型", required = false)
    val instanceType: PipelineInstanceTypeEnum? = null,

    @get:Schema(title = "模版引用方式", required = false)
    val refType: TemplateRefType? = null,
    @get:Schema(title = "用户配置的模版ID", required = false)
    val inputTemplateId: String? = null,
    @get:Schema(title = "用户配置的模版名称", required = false)
    val inputTemplateVersionName: String? = null,
    @get:Schema(title = "用户配置的模版文件路径", required = false)
    val inputTemplateFilePath: String? = null,
    @get:Schema(title = "用户配置的模版引用", required = false)
    val inputTemplateRef: String? = null,

    @get:Schema(title = "模板Id", required = false)
    val templateId: String? = null,
    @get:Schema(title = "模板版本号", required = false)
    val templateVersion: Long? = null,
    @get:Schema(title = "模板版本名称", required = false)
    val templateVersionName: String? = null,
    @get:Schema(title = "创建人", required = false)
    val creator: String? = null,
    @get:Schema(title = "更新人", required = false)
    val updater: String? = null,
    @get:Schema(title = "limit", required = false)
    val limit: Int? = null,
    @get:Schema(title = "offset", required = false)
    val offset: Int? = null
)
