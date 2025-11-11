package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.process.pojo.template.TemplatePipelineStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模板与流水线关联返回体")
data class PipelineTemplateRelatedResp(
    @get:Schema(title = "模板id", required = false)
    val templateId: String,
    @get:Schema(title = "流水线id", required = false)
    val pipelineId: String,
    @get:Schema(title = "流水线名称", required = false)
    val pipelineName: String,
    @get:Schema(title = "流水线版本", required = false)
    val pipelineVersion: Int?,
    @get:Schema(title = "流水线实例版本名称", required = false)
    val pipelineVersionName: String?,
    @get:Schema(title = "流水线版来源的模版版本", required = false)
    val fromTemplateVersion: Long,
    @get:Schema(title = "流水线版本来源的模版版本名称", required = false)
    val fromTemplateVersionName: String,
    @get:Schema(title = "是否有编辑权限", required = false)
    val canEdit: Boolean,
    @get:Schema(title = "流水线模板状态", required = false)
    val status: TemplatePipelineStatus?,
    @get:Schema(title = "是否开启PAC", required = false)
    val enabledPac: Boolean,
    @get:Schema(title = "所属代码库HashId", required = false)
    val repoHashId: String? = null,
    @get:Schema(title = "所属代码库别名", required = false)
    val repoAliasName: String? = null,
    @get:Schema(title = "合并请求链接", required = false)
    val pullRequestUrl: String? = null,
    @get:Schema(title = "模板实例化错误信息", required = false)
    val instanceErrorInfo: String? = null,
    @get:Schema(title = "更新人", required = false)
    val updater: String? = null,
    @get:Schema(title = "更新时间", required = false)
    val updateTime: Long
)
