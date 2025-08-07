package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.process.pojo.template.TemplateRefType
import com.tencent.devops.process.pojo.template.TemplateInstanceStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板实例基本类")
data class PipelineTemplateInstanceBase(
    val baseId: String,
    val projectId: String,
    val templateId: String,
    val templateVersion: Long,
    val useTemplateSetting: Boolean,
    val totalItemNum: Int,
    val successItemNum: Int,
    val failItemNum: Int,
    val description: String?,
    val status: TemplateInstanceStatus,
    val pac: Boolean,
    val targetAction: CodeTargetAction?,
    val type: TemplateInstanceType,
    val repoHashId: String?,
    val targetBranch: String?,
    val templateRefType: TemplateRefType?,
    val templateRef: String?,
    val creator: String,
    val modifier: String,
    val createTime: Long,
    val updateTime: Long
)
