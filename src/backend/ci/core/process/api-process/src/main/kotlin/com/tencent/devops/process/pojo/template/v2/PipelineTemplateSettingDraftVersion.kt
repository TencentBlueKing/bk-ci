package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.pipeline.pojo.setting.BuildCancelPolicy
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模板草稿基础配置版本")
data class PipelineTemplateSettingDraftVersion(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "模板ID", required = true)
    val templateId: String,
    @get:Schema(title = "模板版本号", required = true)
    val version: Long,
    @get:Schema(title = "配置版本号", required = true)
    val settingVersion: Int,
    @get:Schema(title = "草稿版本", required = true)
    val draftVersion: Int,
    @get:Schema(title = "模板名称", required = false)
    val templateName: String?,
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
    /**
     * 将模板草稿配置转换为流水线配置
     */
    fun toPipelineSetting(): PipelineSetting {
        return PipelineSetting(
            projectId = projectId,
            pipelineId = templateId,
            pipelineName = templateName ?: "",
            version = settingVersion,
            desc = desc ?: "",
            labels = labels ?: emptyList(),
            labelNames = emptyList(),
            buildNumRule = buildNumRule,
            successSubscription = successSubscriptionList?.firstOrNull(),
            failSubscription = failSubscriptionList?.firstOrNull(),
            successSubscriptionList = successSubscriptionList,
            failSubscriptionList = failSubscriptionList,
            runLockType = runLockType ?: PipelineRunLockType.SINGLE_LOCK,
            waitQueueTimeMinute = DateTimeUtil.secondToMinute(this.waitQueueTimeSecond ?: 600000),
            maxQueueSize = maxQueueSize ?: PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT,
            concurrencyGroup = concurrencyGroup,
            concurrencyCancelInProgress = concurrencyCancelInProgress ?: false,
            maxConRunningQueueSize = maxConRunningQueueSize,
            failIfVariableInvalid = failIfVariableInvalid ?: false,
            buildCancelPolicy = buildCancelPolicy ?: BuildCancelPolicy.EXECUTE_PERMISSION,
            pipelineAsCodeSettings = pipelineAsCodeSettings
        )
    }
}
