package com.tencent.devops.process.pojo.setting

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.pipeline.pojo.setting.BuildCancelPolicy
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线草稿基础配置版本")
data class PipelineSettingDraftVersion(
    @get:Schema(title = "项目ID", required = false)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "流水线版本号", required = true)
    val version: Int,
    @get:Schema(title = "版本号", required = true)
    val settingVersion: Int,
    @get:Schema(title = "草稿版本", required = true)
    val draftVersion: Int,
    @get:Schema(title = "流水线名称", required = false)
    val pipelineName: String,
    @get:Schema(title = "描述", required = false)
    val desc: String?,
    @get:Schema(title = "标签列表", required = false)
    val labels: List<String>?,
    @get:Schema(title = "构建号生成规则", required = false)
    val buildNumRule: String?,
    @get:Schema(title = "订阅成功通知组", required = false)
    val successSubscriptionList: List<Subscription>?,
    @get:Schema(title = "订阅失败通知组", required = false)
    val failSubscriptionList: List<Subscription>?,
    @get:Schema(title = "Lock 类型", required = false)
    val runLockType: PipelineRunLockType?,
    @get:Schema(title = "最大排队时长", required = false)
    val waitQueueTimeSecond: Int?,
    @get:Schema(title = "最大排队数量", required = false)
    val maxQueueSize: Int?,
    @get:Schema(title = "并发时,设定的group", required = false)
    val concurrencyGroup: String?,
    @get:Schema(title = "并发时,是否相同group取消正在执行的流水线", required = false)
    val concurrencyCancelInProgress: Boolean?,
    @get:Schema(title = "YAML流水线特殊配置", required = false)
    val pipelineAsCodeSettings: PipelineAsCodeSettings?,
    @get:Schema(title = "并发构建数量限制", required = false)
    val maxConRunningQueueSize: Int?,
    @get:Schema(title = "是否配置流水线变量值超长时终止执行", required = false)
    val failIfVariableInvalid: Boolean?,
    @get:Schema(title = "构建取消权限策略", required = false)
    val buildCancelPolicy: BuildCancelPolicy?
) {
    companion object {
        fun convertFromDraftVersion(draft: PipelineSettingDraftVersion): PipelineSettingVersion {
            return PipelineSettingVersion(
                projectId = draft.projectId,
                pipelineId = draft.pipelineId,
                pipelineName = draft.pipelineName,
                version = draft.settingVersion,
                desc = draft.desc,
                labels = draft.labels,
                buildNumRule = draft.buildNumRule,
                successSubscriptionList = draft.successSubscriptionList,
                failSubscriptionList = draft.failSubscriptionList,
                runLockType = draft.runLockType,
                waitQueueTimeMinute = draft.waitQueueTimeSecond,
                maxQueueSize = draft.maxQueueSize,
                concurrencyGroup = draft.concurrencyGroup,
                concurrencyCancelInProgress = draft.concurrencyCancelInProgress,
                maxConRunningQueueSize = draft.maxConRunningQueueSize,
                pipelineAsCodeSettings = draft.pipelineAsCodeSettings,
                failIfVariableInvalid = draft.failIfVariableInvalid,
                buildCancelPolicy = draft.buildCancelPolicy
            )
        }
    }
}
