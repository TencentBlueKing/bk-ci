package com.tencent.devops.common.pipeline

import com.tencent.devops.common.pipeline.enums.TemplateRefType
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceRecommendedVersion
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceTriggerConfig
import com.tencent.devops.common.pipeline.pojo.TemplateVariable
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线实例化描述者")
data class TemplateInstanceDescriptor(
    @get:Schema(title = "模版引用方式", required = false)
    override val templateRefType: TemplateRefType,
    @get:Schema(title = "模板路径", required = false)
    override val templatePath: String? = null,

    @get:Schema(title = "模板版本引用,分支/tag/commit", required = false)
    override val templateRef: String? = null,

    @get:Schema(title = "模板ID", required = false)
    override val templateId: String? = null,

    @get:Schema(title = "模版版本名称", required = false)
    override val templateVersionName: String? = null,

    @get:Schema(title = "模板参数值", required = false)
    override val templateVariables: List<TemplateVariable>? = null,

    /* 模版实例化时触发器变量 */
    @get:Schema(title = "实例化-流水线自定义-触发器配置", required = false)
    val triggerConfigs: List<TemplateInstanceTriggerConfig>? = null,

    @get:Schema(title = "实例化-流水线自定义-推荐版本号", required = false)
    val recommendedVersion: TemplateInstanceRecommendedVersion? = null
) : TemplateDescriptor(
    templateRefType = templateRefType,
    templatePath = templatePath,
    templateRef = templateRef,
    templateId = templateId,
    templateVersionName = templateVersionName,
    templateVariables = templateVariables
)
