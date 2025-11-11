package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模版资源没有版本信息")
data class PTemplateResourceWithoutVersion(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "模板ID", required = true)
    val templateId: String,
    @get:Schema(title = "模板类型", required = true)
    val type: PipelineTemplateType,
    @get:Schema(title = "源模板项目ID", required = false)
    val srcTemplateProjectId: String? = null,
    @get:Schema(title = "源模板ID", required = false)
    val srcTemplateId: String? = null,
    @get:Schema(title = "源模板版本", required = false)
    val srcTemplateVersion: Long? = null,
    @get:Schema(title = "构建参数", required = false)
    val params: List<BuildFormProperty>? = emptyList(),
    @get:Schema(title = "编排", required = false)
    val model: ITemplateModel,
    @get:Schema(title = "编排yaml", required = false)
    val yaml: String?,
    @get:Schema(title = "版本发布描述", required = false)
    val description: String? = null,
    @get:Schema(title = "状态", required = true)
    val status: VersionStatus,
    @get:Schema(title = "分支状态", required = false)
    val branchAction: BranchVersionAction? = null,
    @get:Schema(title = "排序权重，草稿版本权重为100，其他状态的权重为0", required = false)
    val sortWeight: Int? = 0,
    @get:Schema(title = "草稿来源版本,可以通过请求传入", required = false)
    val baseVersion: Long? = null,
    @get:Schema(title = "来源版本名称", required = false)
    val baseVersionName: String? = null,
    @get:Schema(title = "创建人", required = true)
    val creator: String,
    @get:Schema(title = "更新人", required = false)
    val updater: String? = null
) {
    constructor(templateResource: PipelineTemplateResource) : this(
        projectId = templateResource.projectId,
        templateId = templateResource.templateId,
        type = templateResource.type,
        srcTemplateProjectId = templateResource.srcTemplateProjectId,
        srcTemplateId = templateResource.srcTemplateId,
        srcTemplateVersion = templateResource.srcTemplateVersion,
        params = templateResource.params,
        model = templateResource.model,
        yaml = templateResource.yaml,
        description = templateResource.description,
        status = templateResource.status,
        branchAction = templateResource.branchAction,
        sortWeight = templateResource.sortWeight,
        baseVersion = templateResource.baseVersion,
        baseVersionName = templateResource.baseVersionName,
        creator = templateResource.creator,
        updater = templateResource.updater
    )
}
