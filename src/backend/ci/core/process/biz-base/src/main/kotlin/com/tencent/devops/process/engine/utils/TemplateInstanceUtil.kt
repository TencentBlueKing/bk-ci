package com.tencent.devops.process.engine.utils

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceField
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceRecommendedVersion
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceTriggerConfig
import com.tencent.devops.common.pipeline.pojo.TemplateVariable
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSettingGroupType
import com.tencent.devops.process.constant.ProcessTemplateMessageCode
import com.tencent.devops.process.engine.utils.PipelineUtils.getFixedStages
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.utils.FIXVERSION
import com.tencent.devops.process.utils.MAJORVERSION
import com.tencent.devops.process.utils.MINORVERSION

/**
 *  模板实例工具类
 */
object TemplateInstanceUtil {
    /**
     * 通过流水线参数、模板编排和触发器控制生成新Model
     */
    @Suppress("ALL")
    fun instanceModel(
        templateResource: PipelineTemplateResource,
        pipelineName: String,
        labels: List<String>? = null,
        defaultStageTagId: String?,
        staticViews: List<String> = emptyList(),
        templateVariables: List<TemplateVariable>? = null,
        triggerConfigs: List<TemplateInstanceTriggerConfig>? = null,
        recommendedVersion: TemplateInstanceRecommendedVersion? = null,
        overrideTemplateField: TemplateInstanceField? = null
    ): Model {
        if (templateResource.model !is Model) {
            throw ErrorCodeException(
                errorCode = ProcessTemplateMessageCode.ERROR_TEMPLATE_TYPE_MODEL_TYPE_NOT_MATCH
            )
        }
        val templateModel = templateResource.model as Model
        val templateTrigger = templateModel.getTriggerContainer()
        val triggerElements = mergeTriggerElements(
            templateTriggerElements = templateTrigger.elements,
            triggerConfigs = triggerConfigs
        )
        val pipelineParam = mergeParams(
            templateParams = templateTrigger.params,
            templateVariables = templateVariables
        )
        val buildNo = mergeRecommendedVersion(
            pipelineParams = pipelineParam,
            templateBuildNo = templateModel.getTriggerContainer().buildNo,
            recommendedVersion = recommendedVersion
        )
        val triggerContainer = templateTrigger.copy(
            buildNo = buildNo,
            elements = triggerElements,
            params = pipelineParam
        )

        return Model(
            name = pipelineName,
            desc = "",
            stages = getFixedStages(templateModel, triggerContainer, defaultStageTagId),
            labels = labels ?: templateModel.labels,
            instanceFromTemplate = true,
            fromTemplate = true,
            templateId = templateResource.templateId,
            templateVersionName = templateResource.versionName,
            staticViews = staticViews,
            overrideTemplateField = overrideTemplateField
        )
    }

    fun instanceModel(
        model: Model,
        templateResource: PipelineTemplateResource
    ): Model {
        if (templateResource.model !is Model) {
            throw ErrorCodeException(
                errorCode = ProcessTemplateMessageCode.ERROR_TEMPLATE_TYPE_MODEL_TYPE_NOT_MATCH
            )
        }
        val templateModel = templateResource.model as Model
        val triggerContainer = mergeTriggerContainer(
            model = model,
            templateModel = templateModel
        )
        val stages = mutableListOf<Stage>()
        templateModel.stages.forEachIndexed { index, stage ->
            if (index == 0) {
                stages.add(stage.copy(containers = listOf(triggerContainer)))
            } else {
                stages.add(stage)
            }
        }
        return model.copy(
            stages = stages
        )
    }

    fun instanceSetting(
        setting: PipelineSetting,
        templateSetting: PipelineSetting,
        overrideTemplateField: TemplateInstanceField? = null
    ): PipelineSetting {
        // 历史数据,直接使用流水线设置
        if (overrideTemplateField == null) return setting
        val instanceSetting = setting.copy()
        mergeBuildNumRule(
            setting = instanceSetting,
            templateSetting = templateSetting,
            overrideTemplateField = overrideTemplateField
        )
        mergeLabel(
            setting = instanceSetting,
            templateSetting = templateSetting,
            overrideTemplateField = overrideTemplateField
        )
        mergeNotices(
            setting = instanceSetting,
            templateSetting = templateSetting,
            overrideTemplateField = overrideTemplateField
        )
        mergeConcurrency(
            setting = instanceSetting,
            templateSetting = templateSetting,
            overrideTemplateField = overrideTemplateField
        )
        return instanceSetting
    }

    fun getRecommendedVersion(
        buildNo: BuildNo?,
        params: List<BuildFormProperty>,
        overrideTemplateField: TemplateInstanceField?
    ): TemplateInstanceRecommendedVersion? {
        if (overrideTemplateField == null ||
            !overrideTemplateField.overrideRecommendedVersion() ||
            buildNo == null
        ) return null
        val recommendedVersion = TemplateInstanceRecommendedVersion(
            enabled = true,
            buildNo = buildNo
        )
        params.forEach { param ->
            when (param.id) {
                MAJORVERSION -> recommendedVersion.major = param.defaultValue.toString().toIntOrNull() ?: 0
                MINORVERSION -> recommendedVersion.minor = param.defaultValue.toString().toIntOrNull() ?: 0
                FIXVERSION -> recommendedVersion.fix = param.defaultValue.toString().toIntOrNull() ?: 0
            }
        }
        return recommendedVersion
    }

    private fun mergeTriggerContainer(
        model: Model,
        templateModel: Model,
    ): TriggerContainer {
        val triggerElements = mergeTriggerElements(
            templateTriggerElements = templateModel.getTriggerContainer().elements,
            triggerConfigs = model.triggerConfigs
        )
        val pipelineParams = mergeParams(
            templateParams = templateModel.getTriggerContainer().params,
            templateVariables = model.templateVariables
        )
        val buildNo = mergeRecommendedVersion(
            pipelineParams = pipelineParams,
            templateBuildNo = templateModel.getTriggerContainer().buildNo,
            recommendedVersion = model.recommendedVersion
        )
        return templateModel.getTriggerContainer().copy(
            buildNo = buildNo,
            elements = triggerElements,
            params = pipelineParams
        )
    }

    /**
     * 合并触发器
     */
    private fun mergeTriggerElements(
        templateTriggerElements: List<Element>,
        triggerConfigs: List<TemplateInstanceTriggerConfig>?
    ): List<Element> {
        if (triggerConfigs == null) return templateTriggerElements

        val triggerConfigMap = triggerConfigs.filter { it.stepId != null }.associateBy { it.stepId }
        return templateTriggerElements.map { templateTriggerElement ->
            if (templateTriggerElement.stepId.isNullOrEmpty()) {
                templateTriggerElement
            } else {
                val triggerConfig = triggerConfigMap[templateTriggerElement.stepId]
                if (triggerConfig != null) {
                    copyTriggerElement(
                        triggerElement = templateTriggerElement,
                        triggerConfig = triggerConfig
                    )
                } else {
                    templateTriggerElement
                }
            }
        }
    }

    private fun mergeParams(
        templateParams: List<BuildFormProperty>,
        templateVariables: List<TemplateVariable>?
    ): List<BuildFormProperty> {
        if (templateVariables == null) return templateParams

        val templateVariableMap = templateVariables.associateBy { it.key }
        return templateParams.map { templateParam ->
            val templateVariable = templateVariableMap[templateParam.id]
            val pipelineParams = if (templateVariable != null) {
                templateParam.copy(
                    defaultValue = templateVariable.value,
                    required = templateVariable.allowModifyAtStartup ?: templateParam.required
                )
            } else {
                templateParam
            }
            PipelineUtils.cleanOptions(pipelineParams)
        }
    }

    private fun overrideParam(
        templateParam: BuildFormProperty,
        overrideParamIds: List<String>?,
        templateVariable: TemplateVariable?,
    ): Boolean {
        // 覆盖的key存在且变量值类型与模板参数类型一致,则流水线的变量覆盖模版的
        return overrideParamIds != null &&
                overrideParamIds.contains(templateParam.id) &&
                templateVariable != null &&
                templateVariable.value.javaClass == templateParam.defaultValue.javaClass
    }

    private fun overrideTrigger(
        templateTriggerElement: Element,
        overrideTriggerStepIds: List<String>?,
        triggerConfig: TemplateInstanceTriggerConfig?
    ): Boolean {
        return !templateTriggerElement.stepId.isNullOrEmpty() &&
                overrideTriggerStepIds != null &&
                overrideTriggerStepIds.contains(templateTriggerElement.stepId) &&
                triggerConfig != null
    }

    private fun copyTriggerElement(
        triggerElement: Element,
        triggerConfig: TemplateInstanceTriggerConfig
    ): Element {
        triggerConfig.disabled?.let {
            triggerElement.additionalOptions?.enable = !triggerConfig.disabled!!
        }
        return when (triggerElement) {
            is TimerTriggerElement -> {
                triggerConfig.cron?.let { c ->
                    triggerElement.copy(
                        advanceExpression = c
                    )
                } ?: triggerElement
            }

            else -> triggerElement
        }
    }

    private fun mergeBuildNumRule(
        setting: PipelineSetting,
        templateSetting: PipelineSetting,
        overrideTemplateField: TemplateInstanceField
    ) {
        if (overrideTemplateField.overrideSetting(PipelineSettingGroupType.CUSTOM_BUILD_NUM)) return
        setting.buildNumRule = templateSetting.buildNumRule
    }

    private fun mergeLabel(
        setting: PipelineSetting,
        templateSetting: PipelineSetting,
        overrideTemplateField: TemplateInstanceField
    ) {
        if (overrideTemplateField.overrideSetting(PipelineSettingGroupType.LABEL)) return
        setting.labels = templateSetting.labels
        setting.labelNames = templateSetting.labelNames
    }

    private fun mergeNotices(
        setting: PipelineSetting,
        templateSetting: PipelineSetting,
        overrideTemplateField: TemplateInstanceField
    ) {
        if (overrideTemplateField.overrideSetting(PipelineSettingGroupType.NOTICES)) return
        setting.successSubscription = templateSetting.successSubscription
        setting.failSubscription = templateSetting.failSubscription
        setting.successSubscriptionList = templateSetting.successSubscriptionList
        setting.failSubscriptionList = templateSetting.failSubscriptionList
    }

    private fun mergeConcurrency(
        setting: PipelineSetting,
        templateSetting: PipelineSetting,
        overrideTemplateField: TemplateInstanceField
    ) {
        if (overrideTemplateField.overrideSetting(PipelineSettingGroupType.CONCURRENCY)) return
        setting.runLockType = templateSetting.runLockType
        setting.waitQueueTimeMinute = templateSetting.waitQueueTimeMinute
        setting.maxQueueSize = templateSetting.maxQueueSize
        setting.concurrencyGroup = templateSetting.concurrencyGroup
        setting.concurrencyCancelInProgress = templateSetting.concurrencyCancelInProgress
        setting.maxConRunningQueueSize = templateSetting.maxConRunningQueueSize
    }

    private fun mergeRecommendedVersion(
        pipelineParams: List<BuildFormProperty>,
        templateBuildNo: BuildNo?,
        recommendedVersion: TemplateInstanceRecommendedVersion?
    ): BuildNo? {
        if (recommendedVersion == null) return templateBuildNo
        pipelineParams.forEach { param ->
            when (param.id) {
                MAJORVERSION -> param.defaultValue = recommendedVersion.major
                MINORVERSION -> param.defaultValue = recommendedVersion.minor
                FIXVERSION -> param.defaultValue = recommendedVersion.fix
            }
        }
        return recommendedVersion.buildNo
    }
}
