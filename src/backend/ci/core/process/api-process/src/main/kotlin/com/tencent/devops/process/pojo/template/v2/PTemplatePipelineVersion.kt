package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模版流水线关联版本表")
data class PTemplatePipelineVersion(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线Id", required = true)
    val pipelineId: String,
    @get:Schema(title = "流水线版本号", required = true)
    val pipelineVersion: Int,
    @get:Schema(title = "流水线版本名称", required = true)
    val pipelineVersionName: String,
    @get:Schema(title = "实例化类型", required = true)
    val instanceType: PipelineInstanceTypeEnum,
    @get:Schema(title = "推荐版本号", required = false)
    val buildNo: BuildNo?,
    @get:Schema(title = "构建参数", required = false)
    val params: List<BuildFormProperty>?,

    @get:Schema(title = "模版引用方式", required = true)
    val refType: TemplateRefType? = TemplateRefType.ID,
    @get:Schema(title = "用户配置的模版ID", required = false)
    val inputTemplateId: String?,
    @get:Schema(title = "用户配置的模版名称", required = false)
    val inputTemplateVersionName: String?,
    @get:Schema(title = "用户配置的模版文件路径", required = false)
    val inputTemplateFilePath: String? = null,
    @get:Schema(title = "用户配置的模版引用", required = false)
    val inputTemplateRef: String? = null,

    @get:Schema(title = "模板Id", required = true)
    val templateId: String,
    @get:Schema(title = "模板版本号", required = true)
    val templateVersion: Long,
    @get:Schema(title = "模板版本名称", required = true)
    val templateVersionName: String,

    @get:Schema(title = "创建人", required = false)
    val creator: String? = null,
    @get:Schema(title = "更新人", required = false)
    val updater: String? = null
)
