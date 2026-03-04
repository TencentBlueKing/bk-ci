package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模板草稿资源版本")
data class PipelineTemplateResourceDraftVersion(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "模板ID", required = true)
    val templateId: String,
    @get:Schema(title = "版本号", required = true)
    val version: Long,
    @get:Schema(title = "草稿版本", required = true)
    val draftVersion: Int,
    @get:Schema(title = "模板配置版本号", required = true)
    val settingVersion: Int,
    @get:Schema(title = "模板类型", required = true)
    val type: String,
    @get:Schema(title = "来源模板项目ID", required = false)
    val srcTemplateProjectId: String?,
    @get:Schema(title = "来源模板ID", required = false)
    val srcTemplateId: String?,
    @get:Schema(title = "来源模板版本", required = false)
    val srcTemplateVersion: Long?,
    @get:Schema(title = "来源的正式版本", required = false)
    val baseVersion: Long?,
    @get:Schema(title = "来源的正式版本名称", required = false)
    val baseVersionName: String?,
    @get:Schema(title = "来源的草稿版本", required = false)
    val baseDraftVersion: Int?,
    @get:Schema(title = "模板参数", required = false)
    val params: String?,
    @get:Schema(title = "模板模型", required = false)
    val model: Model,
    @get:Schema(title = "YAML编排内容", required = false)
    val yaml: String?,
    @get:Schema(title = "创建者", required = true)
    val creator: String,
    @get:Schema(title = "创建时间", required = true)
    val createTime: Long,
    @get:Schema(title = "最近更新人", required = false)
    val updater: String?,
    @get:Schema(title = "更新时间", required = true)
    val updateTime: Long
) {
    /**
     * 将草稿版本转换为模板资源
     */
    fun convertTemplateResource(): PipelineTemplateResource {
        return PipelineTemplateResource(
            projectId = projectId,
            templateId = templateId,
            type = PipelineTemplateType.valueOf(type),
            storeStatus = TemplateStatusEnum.NEVER_PUBLISHED,
            settingVersion = settingVersion,
            version = version,
            number = -1, // 草稿版本使用默认排序号
            versionNum = null,
            settingVersionNum = null,
            pipelineVersion = null,
            triggerVersion = null,
            srcTemplateProjectId = srcTemplateProjectId,
            srcTemplateId = srcTemplateId,
            srcTemplateVersion = srcTemplateVersion,
            baseVersion = baseVersion,
            baseVersionName = baseVersionName,
            params = null, // 草稿版本暂不支持构建参数
            model = model,
            yaml = yaml,
            status = VersionStatus.COMMITTING,
            branchAction = null,
            description = null,
            sortWeight = 100,
            creator = creator,
            updater = updater,
            releaseTime = null,
            createdTime = createTime,
            updateTime = updateTime,
            draftVersion = draftVersion
        )
    }
}
