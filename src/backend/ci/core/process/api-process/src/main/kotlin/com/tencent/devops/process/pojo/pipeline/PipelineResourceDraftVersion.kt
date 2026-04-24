package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.VersionStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "流水线草稿资源版本")
data class PipelineResourceDraftVersion(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "版本号", required = true)
    val version: Int,
    @get:Schema(title = "草稿版本", required = true)
    val draftVersion: Int,
    @get:Schema(title = "流水线模型", required = false)
    val model: Model,
    @get:Schema(title = "YAML编排内容", required = false)
    val yaml: String?,
    @get:Schema(title = "YAML编排版本", required = false)
    val yamlVersion: String?,
    @get:Schema(title = "关联的流水线设置版本号", required = false)
    val settingVersion: Int?,
    @get:Schema(title = "来源的正式版本", required = false)
    val baseVersion: Int?,
    @get:Schema(title = "来源的正式版本名称", required = false)
    val baseVersionName: String?,
    @get:Schema(title = "来源的草稿版本", required = false)
    val baseDraftVersion: Int?,
    @get:Schema(title = "创建者", required = false)
    val creator: String?,
    @get:Schema(title = "创建时间", required = true)
    val createTime: LocalDateTime,
    @get:Schema(title = "最近更新人", required = false)
    val updater: String?,
    @get:Schema(title = "更新时间", required = true)
    val updateTime: LocalDateTime
) {
    companion object {
        fun convertDraftToVersion(draft: PipelineResourceDraftVersion): PipelineResourceVersion {
            return PipelineResourceVersion(
                projectId = draft.projectId,
                pipelineId = draft.pipelineId,
                version = draft.version,
                model = draft.model,
                yaml = draft.yaml,
                yamlVersion = draft.yamlVersion,
                versionName = null,
                creator = draft.creator ?: "",
                createTime = draft.createTime,
                updater = draft.updater,
                updateTime = draft.updateTime,
                versionNum = null,
                pipelineVersion = null,
                triggerVersion = null,
                settingVersion = draft.settingVersion,
                status = VersionStatus.COMMITTING,
                branchAction = null,
                baseVersion = draft.baseVersion,
                draftVersion = draft.draftVersion
            )
        }
    }
}
