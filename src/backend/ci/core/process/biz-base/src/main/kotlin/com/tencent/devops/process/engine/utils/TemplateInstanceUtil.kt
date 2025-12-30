package com.tencent.devops.process.engine.utils

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
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
        labels: List<String>? = null,
        defaultStageTagId: String?,
        staticViews: List<String> = emptyList(),
        buildNo: BuildNo?,
        params: List<BuildFormProperty>?,
        triggerConfigs: List<TemplateInstanceTriggerConfig>? = null,
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
            triggerConfigs = triggerConfigs
        )
        val pipelineParams = mergeParams(
            templateParams = templateTrigger.params,
            pipelineParams = params,
            overrideTemplateField = overrideTemplateField
        )
        val buildNo = mergeBuildNo(
            templateBuildNo = templateTrigger.buildNo,
            pipelineBuildNo = buildNo,
            overrideTemplateField = overrideTemplateField
        )
        val triggerContainer = templateTrigger.copy(
            buildNo = buildNo,
            elements = triggerElements,
            params = pipelineParams
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
            overrideTemplateField = overrideTemplateField
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
        mergeBuildCancelPolicy(instanceSetting, templateSetting, overrideTemplateField)
        return instanceSetting
    }

    private fun mergeTriggerContainer(
        model: Model,
        templateModel: Model,
    ): TriggerContainer {
        val templateTrigger = templateModel.getTriggerContainer()
        val template = model.template
        val triggerElements = mergeTriggerElements(
            templateTriggerElements = templateTrigger.elements,
            triggerConfigs = template?.triggerConfigs
        )
        val pipelineParams = mergeParams(
            templateParams = templateTrigger.params,
            templateVariables = template?.templateVariables
        )
        val buildNo = mergeRecommendedVersion(
            pipelineParams = pipelineParams,
            templateParams = templateTrigger.params,
            templateBuildNo = templateModel.getTriggerContainer().buildNo,
            recommendedVersion = template?.recommendedVersion
        )
        return templateTrigger.copy(
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
                triggerConfigMap[templateTriggerElement.stepId]?.let {
                    copyTriggerElement(
                        triggerElement = templateTriggerElement,
                        triggerConfig = it
                    )
                } ?: templateTriggerElement
            }
        }
    }

    private fun mergeParams(
        templateParams: List<BuildFormProperty>,
        pipelineParams: List<BuildFormProperty>?,
        overrideTemplateField: TemplateInstanceField?
    ): List<BuildFormProperty> {
        // 如果没有流水线参数，直接返回模板参数
        if (pipelineParams == null) return templateParams

        // 检查是否有常量参数被非法覆盖
        validateConstParamOverride(
            templateParams = templateParams,
            overrideTemplateField = overrideTemplateField
        )

        val pipelineParamMap = pipelineParams.associateBy { it.id }
        val mergedParams = templateParams.map { templateParam ->
            mergeSingleParam(
                templateParam = templateParam,
                pipelineParamMap = pipelineParamMap,
                overrideTemplateField = overrideTemplateField
            )
        }
        return PipelineUtils.cleanOptions(mergedParams)
    }

    private fun mergeParams(
        templateParams: List<BuildFormProperty>,
        templateVariables: List<TemplateVariable>?
    ): List<BuildFormProperty> {
        if (templateVariables == null) return templateParams

        validateConstParamOverride(
            templateParams = templateParams,
            templateVariables = templateVariables
        )

        val templateVariableMap = templateVariables.associateBy { it.key }
        val mergedParams = templateParams.map { templateParam ->
            mergeSingeParam(templateVariableMap, templateParam)
        }
        return PipelineUtils.cleanOptions(mergedParams)
    }

    /**
     * 验证常量参数是否被非法覆盖
     */
    private fun validateConstParamOverride(
        templateParams: List<BuildFormProperty>,
        overrideTemplateField: TemplateInstanceField?
    ) {
        val overrideConstParamIds = templateParams
            .filter { isOverrideConstParam(it, overrideTemplateField) }
            .map { it.id }

        if (overrideConstParamIds.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_INSTANCE_OVERRIDE_CONST,
                params = arrayOf(overrideConstParamIds.joinToString { "[$it]" })
            )
        }
    }

    /**
     * 验证常量参数是否被非法覆盖
     * 如果存在常量参数被覆盖的情况，抛出异常
     */
    private fun validateConstParamOverride(
        templateParams: List<BuildFormProperty>,
        templateVariables: List<TemplateVariable>
    ) {
        val constParamIds = templateParams
            .filter { it.constant == true || !it.required }
            .map { it.id }

        val overrideConstParamIds = templateVariables
            .filter { constParamIds.contains(it.key) }

        if (overrideConstParamIds.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_INSTANCE_OVERRIDE_CONST,
                params = arrayOf(overrideConstParamIds.joinToString { "[${it.key}]" })
            )
        }
    }

    /**
     * 合并单个参数
     */
    private fun mergeSingleParam(
        templateParam: BuildFormProperty,
        pipelineParamMap: Map<String, BuildFormProperty>,
        overrideTemplateField: TemplateInstanceField?
    ): BuildFormProperty {
        val pipelineParam = pipelineParamMap[templateParam.id] ?: return templateParam

        // 如果没有对应的流水线参数，直接返回模板参数
        val overrideParam = overrideTemplateField?.overrideParam(templateParam.id) == true
        val overrideBuildNo = overrideTemplateField?.overrideBuildNo() == true

        val defaultValue = determineDefaultValue(
            templateParam = templateParam,
            pipelineParam = pipelineParam,
            overrideParam = overrideParam,
            overrideBuildNo = overrideBuildNo
        )

        return pipelineParam.copy(defaultValue = defaultValue)
    }

    private fun mergeSingeParam(
        templateVariableMap: Map<String, TemplateVariable>,
        templateParam: BuildFormProperty
    ): BuildFormProperty {
        // 如果是常量参数或者其他变量,则直接反正模版参数
        if (templateParam.constant == true || !templateParam.required) {
            return templateParam
        }
        val templateVariable = templateVariableMap[templateParam.id] ?: run {
            // 如果yaml中变量没有声明,表示值和入参都跟随模版,不能直接使用模版的require,应该使用asInstanceInput
            val asInstanceInput = templateParam.asInstanceInput
            return if (asInstanceInput == null) {
                templateParam
            } else {
                templateParam.copy(required = asInstanceInput)
            }
        }

        val defaultValue = determineDefaultValue(
            templateParam = templateParam,
            templateVariable = templateVariable
        )
        // 用templateVariable覆盖模板的默认值
        return templateParam.copy(
            defaultValue = defaultValue,
            required = templateVariable.allowModifyAtStartup ?: templateParam.required
        )
    }

    /**
     * 确定参数的默认值
     */
    private fun determineDefaultValue(
        templateParam: BuildFormProperty,
        pipelineParam: BuildFormProperty,
        overrideParam: Boolean,
        overrideBuildNo: Boolean
    ): Any {
        return when (templateParam.id) {
            MAJORVERSION, MINORVERSION, FIXVERSION -> {
                // 版本号参数：根据overrideBuildNo决定使用模板值还是流水线值
                if (overrideBuildNo) pipelineParam.defaultValue else templateParam.defaultValue
            }
            else -> {
                // 其他参数：根据overrideParam决定使用模板值还是流水线值
                if (overrideParam) pipelineParam.defaultValue else templateParam.defaultValue
            }
        }
    }

    /**
     * 确定实例参数的默认值
     */
    private fun determineDefaultValue(
        templateParam: BuildFormProperty,
        templateVariable: TemplateVariable
    ): Any {
        logger.info(
            "template instance default value|$templateParam|$templateVariable|${templateVariable.value?.javaClass}"
        )
        return when {
            // 如果实例没有声明默认值,则使用模版的值
            templateVariable.value == null -> templateParam.defaultValue
            // 从yaml转换过来的值,在yaml中不知道变量类型,所以默认都是字符串,需要进行转换
            templateParam.type == BuildFormPropertyType.BOOLEAN && templateVariable.value is String -> {
                (templateVariable.value as String?).toBoolean()
            }
            templateParam.type == BuildFormPropertyType.MULTIPLE && templateVariable.value is List<*> -> {
                (templateVariable.value as List<*>).joinToString(",")
            }
            else -> templateVariable.value!!
        }
    }

    private fun isOverrideConstParam(
        templateParam: BuildFormProperty,
        overrideTemplateField: TemplateInstanceField?
    ): Boolean {
        return (templateParam.constant == true || !templateParam.required) &&
                overrideTemplateField?.overrideParam(templateParam.id) == true
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
                triggerElement.copy(
                    advanceExpression = triggerConfig.cron ?: triggerElement.advanceExpression,
                    startParams = triggerConfig.variables?.let {
                        JsonUtil.toJson(it, false)
                    } ?: triggerElement.startParams
                )
            }

            else -> triggerElement
        }
    }

    private fun mergeBuildNumRule(
        setting: PipelineSetting,
        templateSetting: PipelineSetting,
        overrideTemplateField: TemplateInstanceField
    ) {
        // 如果"自定义构建号"这个设置项不被实例覆盖，则使用模板的设置
        if (!overrideTemplateField.overrideSetting(PipelineSettingGroupType.CUSTOM_BUILD_NUM)) {
            setting.buildNumRule = templateSetting.buildNumRule
        }
    }

    private fun mergeLabel(
        setting: PipelineSetting,
        templateSetting: PipelineSetting,
        overrideTemplateField: TemplateInstanceField
    ) {
        // 如果"标签"这个设置项不被实例覆盖，则使用模板的设置
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
        // 如果"通知"这个设置项不被实例覆盖，则使用模板的设置
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
        // 如果"并发"这个设置项不被实例覆盖，则使用模板的设置
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
        // 如果"变量检查"这个设置项不被实例覆盖，则使用模板的设置
        if (!overrideTemplateField.overrideSetting(PipelineSettingGroupType.FAIL_IF_VARIABLE_INVALID)) {
            setting.failIfVariableInvalid = templateSetting.failIfVariableInvalid
        }
    }

    private fun mergeBuildCancelPolicy(
        setting: PipelineSetting,
        templateSetting: PipelineSetting,
        overrideTemplateField: TemplateInstanceField
    ) {
        // 如果"构建取消策略"这个设置项不被实例覆盖，则使用模板的设置
        if (!overrideTemplateField.overrideSetting(PipelineSettingGroupType.BUILD_CANCEL_POLICY)) {
            setting.buildCancelPolicy = templateSetting.buildCancelPolicy
        }
    }

    private fun mergeBuildNo(
        templateBuildNo: BuildNo?,
        pipelineBuildNo: BuildNo?,
        overrideTemplateField: TemplateInstanceField?
    ): BuildNo? {
        // 如果模版关闭推荐版本号, 则实例也应该关闭
        if (templateBuildNo == null) {
            return null
        }
        // 如果实例关闭推荐版本号, 以模版的为准
        if (pipelineBuildNo == null) {
            return templateBuildNo
        }
        val overrideBuildNo = overrideTemplateField?.overrideBuildNo() == true
        val buildNo = if (overrideBuildNo) {
            pipelineBuildNo.buildNo
        } else {
            templateBuildNo.buildNo
        }
        return pipelineBuildNo.copy(buildNo = buildNo)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun mergeRecommendedVersion(
        pipelineParams: List<BuildFormProperty>,
        templateParams: List<BuildFormProperty>,
        templateBuildNo: BuildNo?,
        recommendedVersion: TemplateInstanceRecommendedVersion?
    ): BuildNo? {
        // 如果模版关闭推荐版本号, 则实例也应该关闭
        if (templateBuildNo == null) return null
        if (recommendedVersion == null) return templateBuildNo
        val templateParamMap = templateParams.associateBy { it.id }
        pipelineParams.forEach { param ->
            when (param.id) {
                MAJORVERSION -> param.defaultValue =
                    recommendedVersion.major ?: templateParamMap[MAJORVERSION]?.defaultValue ?: 0

                MINORVERSION -> param.defaultValue =
                    recommendedVersion.minor ?: templateParamMap[MINORVERSION]?.defaultValue ?: 0

                FIXVERSION -> param.defaultValue =
                    recommendedVersion.fix ?: templateParamMap[FIXVERSION]?.defaultValue ?: 0
            }
        }

        return templateBuildNo.copy(
            required = recommendedVersion.allowModifyAtStartup ?: templateBuildNo.required,
            buildNo = recommendedVersion.buildNo?.buildNo ?: templateBuildNo.buildNo
        )
    }

    @Suppress("CyclomaticComplexMethod")
    fun getRecommendedVersion(
        pipelineBuildNo: BuildNo?,
        pipelineParams: List<BuildFormProperty>,
        templateBuildNo: BuildNo?,
        overrideTemplateField: TemplateInstanceField?
    ): TemplateInstanceRecommendedVersion? {
        // 如果模版关闭推荐版本号, 则实例也应该关闭
        if (templateBuildNo == null || pipelineBuildNo == null) return null
        val overrideBuildNo = overrideTemplateField?.overrideBuildNo() == true

        val recommendedVersion = TemplateInstanceRecommendedVersion(
            allowModifyAtStartup = pipelineBuildNo.required
        )
        if (overrideBuildNo) {
            recommendedVersion.buildNo = TemplateInstanceRecommendedVersion.BuildNo(pipelineBuildNo.buildNo)
            pipelineParams.forEach { param ->
                when (param.id) {
                    MAJORVERSION -> recommendedVersion.major = param.defaultValue.toString().toIntOrNull() ?: 0
                    MINORVERSION -> recommendedVersion.minor = param.defaultValue.toString().toIntOrNull() ?: 0
                    FIXVERSION -> recommendedVersion.fix = param.defaultValue.toString().toIntOrNull() ?: 0
                }
            }
        }
        return recommendedVersion
    }

    /**
     * 获取转换成yaml时的实例化参数
     *
     * yaml中只展示与模版不同的参数
     *   - 值不跟随模版的值,需要在yaml中声明
     *   - 是否入参不一致,需要在yaml中声明
     *
     * 通过ui方式实例化时,前端会把所有的参数都传过来，这时需要根据对比模版与流水线的参数,来决定是否需要声明
     */
    fun getTemplateVariables(
        pipelineParams: List<BuildFormProperty>,
        templateParams: List<BuildFormProperty>,
        overrideTemplateField: TemplateInstanceField?
    ): List<TemplateVariable> {
        val templateVariables = mutableListOf<TemplateVariable>()
        val pipelineParamMap = pipelineParams.associateBy { it.id }
        templateParams.filterNot {
            it.constant == true || !it.required || VERSION_PARAMS.contains(it.id)
        }.forEach { templateParam ->
            val pipelineParam = pipelineParamMap[templateParam.id] ?: return@forEach
            // 是否覆盖模版的值
            val overrideParam = overrideTemplateField?.overrideParam(templateParam.id) == true
            // 不跟随模版,则使用流水线的值,需要在yaml中声明
            val defaultValue = if (overrideParam) {
                when (templateParam.type) {
                    BuildFormPropertyType.MULTIPLE -> pipelineParam.defaultValue.toString().split(",").toList()
                    else -> pipelineParam.defaultValue
                }
            } else {
                null
            }
            val templateVariable = TemplateVariable(
                key = templateParam.id,
                value = defaultValue,
                allowModifyAtStartup = pipelineParam.required
            )
            templateVariables.add(templateVariable)
        }
        return templateVariables
    }

    fun getTriggerConfigs(
        elements: List<Element>,
        overrideTemplateField: TemplateInstanceField?
    ): List<TemplateInstanceTriggerConfig> {
        return elements.filter { element ->
            element.stepId != null && overrideTemplateField?.overrideTrigger(element.stepId!!) ?: false
        }.map { TemplateInstanceTriggerConfig(it) }
    }

    private val logger = LoggerFactory.getLogger(TemplateInstanceUtil::class.java)
    private val VERSION_PARAMS = listOf(MAJORVERSION, MINORVERSION, FIXVERSION)
}
