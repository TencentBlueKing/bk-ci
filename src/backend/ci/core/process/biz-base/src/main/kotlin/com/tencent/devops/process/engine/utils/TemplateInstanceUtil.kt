package com.tencent.devops.process.engine.utils

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.TemplateInstanceDescriptor
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
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
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.utils.PipelineUtils.getFixedStages
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.utils.FIXVERSION
import com.tencent.devops.process.utils.MAJORVERSION
import com.tencent.devops.process.utils.MINORVERSION
import org.slf4j.LoggerFactory

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
        defaultStageTagId: String?,
        buildNo: BuildNo?,
        params: List<BuildFormProperty>?,
        triggerConfigs: List<TemplateInstanceTriggerConfig>?,
        overrideTemplateField: TemplateInstanceField? = null,
        template: TemplateInstanceDescriptor? = null
    ): Model {
        // 前端会把所有的参数都传过来，这里只需要保留流水线自定义的参数,ui方式实例化,参数默认都是自定义
        // 以下变量为流水线自身的，不跟随模板，会对模板的变量默认值，进行覆盖。
        val templateVariables = params?.filter {
            overrideTemplateField?.overrideParam(it.id) ?: true
        }?.map { TemplateVariable(it) }

        // 前端会把所有的触发器都传过来,这里只需要保留流水线自定义的触发器,ui方式实例化,触发器默认继承模版
        // 以下触发器配置为流水线自定义的触发器，不跟随模板，会对流水线模板的触发器配置进行覆盖
        val overrideTemplateTriggerConfigs = triggerConfigs?.filter {
            it.stepId != null && overrideTemplateField?.overrideTrigger(it.stepId!!) ?: false
        }

        // 是否覆盖推荐版本号
        val recommendedVersion = getRecommendedVersion(
            buildNo = buildNo,
            params = params ?: emptyList(),
            overrideTemplateField = overrideTemplateField
        )
        return instanceModel(
            templateResource = templateResource,
            pipelineName = pipelineName,
            defaultStageTagId = defaultStageTagId,
            templateVariables = templateVariables,
            overrideTemplateTriggerConfigs = overrideTemplateTriggerConfigs,
            recommendedVersion = recommendedVersion,
            overrideTemplateField = overrideTemplateField,
            template = template
        )
    }
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
        overrideTemplateTriggerConfigs: List<TemplateInstanceTriggerConfig>? = null,
        recommendedVersion: TemplateInstanceRecommendedVersion? = null,
        overrideTemplateField: TemplateInstanceField? = null,
        template: TemplateInstanceDescriptor? = null
    ): Model {
        if (templateResource.model !is Model) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_TYPE_MODEL_TYPE_NOT_MATCH
            )
        }
        val templateModel = templateResource.model as Model
        val templateTrigger = templateModel.getTriggerContainer()
        val triggerElements = mergeTriggerElements(
            templateTriggerElements = templateTrigger.elements,
            overrideTemplateTriggerConfigs = overrideTemplateTriggerConfigs
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
            params = pipelineParams.toMutableList()
        )

        return Model(
            name = pipelineName,
            desc = "",
            stages = getFixedStages(templateModel, triggerContainer, defaultStageTagId),
            labels = labels ?: templateModel.labels,
            instanceFromTemplate = true,
            staticViews = staticViews,
            templateId = templateResource.templateId,
            template = template,
            overrideTemplateField = overrideTemplateField,
            publicVarGroups = templateModel.publicVarGroups
        )
    }

    fun instanceModel(
        model: Model,
        templateResource: PipelineTemplateResource
    ): Model {
        if (templateResource.model !is Model) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_TYPE_MODEL_TYPE_NOT_MATCH
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
        // 历史数据或模板未实例化，直接使用流水线自身的设置
        if (overrideTemplateField == null) {
            return setting
        }
        // 创建一个新的设置对象副本，避免修改原始 setting
        val instanceSetting = setting.copy()
        // 逐个合并配置项
        mergeBuildNumRule(instanceSetting, templateSetting, overrideTemplateField)
        mergeLabel(instanceSetting, templateSetting, overrideTemplateField)
        mergeNotices(instanceSetting, templateSetting, overrideTemplateField)
        mergeConcurrency(instanceSetting, templateSetting, overrideTemplateField)
        mergeFailIfVariableInvalid(instanceSetting, templateSetting, overrideTemplateField)
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
            overrideTemplateTriggerConfigs = model.template?.triggerConfigs
        )
        val pipelineParams = mergeParams(
            templateParams = templateModel.getTriggerContainer().params,
            templateVariables = model.template?.templateVariables
        )
        val buildNo = mergeRecommendedVersion(
            pipelineParams = pipelineParams,
            templateBuildNo = templateModel.getTriggerContainer().buildNo,
            recommendedVersion = model.template?.recommendedVersion
        )
        return templateModel.getTriggerContainer().copy(
            buildNo = buildNo,
            elements = triggerElements,
            params = pipelineParams.toMutableList()
        )
    }

    /**
     * 合并触发器
     */
    private fun mergeTriggerElements(
        templateTriggerElements: List<Element>,
        overrideTemplateTriggerConfigs: List<TemplateInstanceTriggerConfig>?
    ): List<Element> {
        if (overrideTemplateTriggerConfigs == null) return templateTriggerElements

        val triggerConfigMap = overrideTemplateTriggerConfigs.filter { it.stepId != null }.associateBy { it.stepId }
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

        val constParamIds = templateParams.filter { it.constant == true }.map { it.id }
        // 判断是否覆盖常量
        val overrideConstParamIds = templateVariables.filter { constParamIds.contains(it.key)}
        if (overrideConstParamIds.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_INSTANCE_OVERRIDE_CONST,
                params = arrayOf(overrideConstParamIds.joinToString { "[${it.key}]" })
            )
        }

        val templateVariableMap = templateVariables.associateBy { it.key }
        return templateParams.map { templateParam ->
            val templateVariable = templateVariableMap[templateParam.id]

            val pipelineParams = if (templateVariable != null) {
                // 用templateVariable覆盖模板的默认值
                templateParam.copy(
                    defaultValue = getPipelineParamDefaultValue(
                        templateParam = templateParam,
                        templateVariable = templateVariable
                    ),
                    required = templateVariable.allowModifyAtStartup ?: templateParam.required
                )
            } else {
                templateParam
            }
            PipelineUtils.cleanOptions(pipelineParams)
        }
    }

    private fun getPipelineParamDefaultValue(
        templateParam: BuildFormProperty,
        templateVariable: TemplateVariable
    ): Any {
        logger.info(
            "template instance default value|$templateParam|$templateVariable|${templateVariable.value.javaClass}"
        )
        return when {
            // 从yaml转换过来的值,在yaml中不知道变量类型,所以默认都是字符串,需要进行转换
            templateParam.type == BuildFormPropertyType.BOOLEAN && templateVariable.value is String -> {
                (templateVariable.value as String?).toBoolean()
            }
            templateParam.type == BuildFormPropertyType.MULTIPLE && templateVariable.value is List<*> -> {
                (templateVariable.value as List<*>).joinToString(",")
            }
            else -> templateVariable.value
        }
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
        // 如果“自定义构建号”这个设置项不被实例覆盖，则使用模板的设置
        if (!overrideTemplateField.overrideSetting(PipelineSettingGroupType.CUSTOM_BUILD_NUM)) {
            setting.buildNumRule = templateSetting.buildNumRule
        }
    }

    private fun mergeLabel(
        setting: PipelineSetting,
        templateSetting: PipelineSetting,
        overrideTemplateField: TemplateInstanceField
    ) {
        // 如果“标签”这个设置项不被实例覆盖，则使用模板的设置
        if (!overrideTemplateField.overrideSetting(PipelineSettingGroupType.LABEL)) {
            setting.labels = templateSetting.labels
            setting.labelNames = templateSetting.labelNames
        }
    }

    private fun mergeNotices(
        setting: PipelineSetting,
        templateSetting: PipelineSetting,
        overrideTemplateField: TemplateInstanceField
    ) {
        // 如果“通知”这个设置项不被实例覆盖，则使用模板的设置
        if (!overrideTemplateField.overrideSetting(PipelineSettingGroupType.NOTICES)) {
            setting.successSubscription = templateSetting.successSubscription
            setting.failSubscription = templateSetting.failSubscription
            setting.successSubscriptionList = templateSetting.successSubscriptionList
            setting.failSubscriptionList = templateSetting.failSubscriptionList
        }
    }

    private fun mergeConcurrency(
        setting: PipelineSetting,
        templateSetting: PipelineSetting,
        overrideTemplateField: TemplateInstanceField
    ) {
        // 如果“并发”这个设置项不被实例覆盖，则使用模板的设置
        if (!overrideTemplateField.overrideSetting(PipelineSettingGroupType.CONCURRENCY)) {
            setting.runLockType = templateSetting.runLockType
            setting.waitQueueTimeMinute = templateSetting.waitQueueTimeMinute
            setting.maxQueueSize = templateSetting.maxQueueSize
            setting.concurrencyGroup = templateSetting.concurrencyGroup
            setting.concurrencyCancelInProgress = templateSetting.concurrencyCancelInProgress
            setting.maxConRunningQueueSize = templateSetting.maxConRunningQueueSize
        }
    }

    private fun mergeFailIfVariableInvalid(
        setting: PipelineSetting,
        templateSetting: PipelineSetting,
        overrideTemplateField: TemplateInstanceField
    ) {
        // 如果“变量检查”这个设置项不被实例覆盖，则使用模板的设置
        if (!overrideTemplateField.overrideSetting(PipelineSettingGroupType.FAIL_IF_VARIABLE_INVALID)) {
            setting.failIfVariableInvalid = templateSetting.failIfVariableInvalid
        }
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

    private val logger = LoggerFactory.getLogger(TemplateInstanceUtil::class.java)
}
