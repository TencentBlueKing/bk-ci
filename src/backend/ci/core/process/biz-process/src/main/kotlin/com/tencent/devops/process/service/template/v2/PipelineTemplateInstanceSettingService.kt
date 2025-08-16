package com.tencent.devops.process.service.template.v2

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSubscriptionType
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.process.engine.service.PipelineInfoExtService
import com.tencent.devops.process.yaml.utils.NotifyTemplateUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineTemplateInstanceSettingService @Autowired constructor(
    private val pipelineInfoExtService: PipelineInfoExtService,
    private val pipelineTemplateSettingService: PipelineTemplateSettingService
) {
    fun getTemplateInstanceDefaultSetting(
        projectId: String,
        pipelineId: String,
        pipelineName: String
    ): PipelineSetting {
        val failNotifyTypes = pipelineInfoExtService.failNotifyChannel()
        val failType = failNotifyTypes.split(",").filter { i -> i.isNotBlank() }
            .map { type -> PipelineSubscriptionType.valueOf(type) }.toSet()
        val failSubscription = Subscription(
            types = failType,
            groups = emptySet(),
            users = "\${{ci.actor}}",
            content = NotifyTemplateUtils.getCommonShutdownFailureContent()
        )
        return PipelineSetting.defaultSetting(
            projectId = projectId, pipelineId = pipelineId, pipelineName = pipelineName,
            maxPipelineResNum = null, failSubscription = failSubscription
        )
    }

    fun getTemplateInstanceSetting(
        projectId: String,
        templateId: String,
        settingVersion: Int,
        pipelineId: String,
        pipelineName: String,
        pipelineLabels: List<String>?,
        enabledPac: Boolean,
        version: Int
    ): PipelineSetting {
        val pipelineTemplateSetting = pipelineTemplateSettingService.getPipelineTemplateSetting(
            projectId = projectId,
            templateId = templateId,
            settingVersion = settingVersion
        )
        return with(pipelineTemplateSetting) {
            PipelineSetting(
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                version = version,
                labels = pipelineLabels ?: labels,
                buildNumRule = buildNumRule,
                successSubscriptionList = successSubscriptionList,
                failSubscriptionList = failSubscriptionList,
                runLockType = runLockType,
                waitQueueTimeMinute = waitQueueTimeMinute,
                maxQueueSize = maxQueueSize,
                concurrencyGroup = concurrencyGroup,
                concurrencyCancelInProgress = concurrencyCancelInProgress,
                maxConRunningQueueSize = maxConRunningQueueSize,
                maxPipelineResNum = maxPipelineResNum,
                pipelineAsCodeSettings = pipelineAsCodeSettings?.copy(
                    enable = enabledPac
                ) ?: PipelineAsCodeSettings(enable = enabledPac)
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateInstanceSettingService::class.java)
    }
}
