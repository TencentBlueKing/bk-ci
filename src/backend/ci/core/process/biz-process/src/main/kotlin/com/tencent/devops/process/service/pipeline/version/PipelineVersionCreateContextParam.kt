package com.tencent.devops.process.service.pipeline.version

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线版本创建上下文参数")
data class PipelineVersionCreateContextParam(
    @get:Schema(title = "用户ID", required = true)
    val userId: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "渠道")
    val channelCode: ChannelCode,
    @get:Schema(title = "版本,发布时才有值", required = true)
    val version: Int?,
    @get:Schema(title = "流水线编排信息", required = true)
    val model: Model,
    @get:Schema(title = "YAML编排内容", required = false)
    val yaml: String?,
    @get:Schema(title = "该版本的来源版本（空时一定为主路径）", required = false)
    val baseVersion: Int? = null,
    @get:Schema(title = "该版本的来源草稿版本", required = false)
    val baseDraftVersion: Int? = null,
    @get:Schema(title = "版本变更说明", required = false)
    val description: String? = null,
    @get:Schema(title = "流水线设置,没有版本信息", required = true)
    val pipelineSettingWithoutVersion: PipelineSetting,
    @get:Schema(title = "版本状态", required = true)
    val versionStatus: VersionStatus,
    @get:Schema(title = "版本变更动作", required = true)
    val versionAction: PipelineVersionAction,
    @get:Schema(title = "代码库ID")
    val repoHashId: String? = null,
    @get:Schema(title = "分支名,代码库推送的分支/分支版本时,发布的分支名", required = false)
    val branchName: String? = null
)
