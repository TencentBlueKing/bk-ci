package com.tencent.devops.process.service.creative

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.utils.PipelineUtils
import org.springframework.stereotype.Service

/**
 * 变量覆盖 P1：只改 defaultValue。
 * 参考 TemplateInstanceUtil 的实现细节，但校验规则独立（spec §4.3.1）：
 * - 覆盖 required=false 的变量：允许（Q6 明确要求）
 * - 覆盖 constant=true 的变量：禁止（常量语义即不可改）
 * - 类型转换：按 BuildFormPropertyType 做 BOOLEAN/MULTIPLE 等转换
 */
@Service
class CreativeFlowVariableOverrideService {

    fun applyOverrides(model: Model, overrides: Map<String, String>?): Model {
        if (overrides.isNullOrEmpty()) return model

        val trigger = model.stages.firstOrNull()?.containers?.firstOrNull() as? TriggerContainer
            ?: return model
        val params = trigger.params.toMutableList()
        val paramMap = params.associateBy { it.id }.toMutableMap()

        for ((key, value) in overrides) {
            val prop = paramMap[key] ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_VARIABLE_OVERRIDE_INVALID,
                params = arrayOf(key),
                defaultMessage = "Variable '$key' not found in model"
            )

            if (prop.constant == true) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_VARIABLE_OVERRIDE_INVALID,
                    params = arrayOf(key),
                    defaultMessage = "Variable '$key' is a constant and cannot be overridden"
                )
            }

            val convertedValue = determineDefaultValue(value, prop.type)
            val updatedProp = prop.copy(defaultValue = convertedValue)
            val updatedWithOptions = cleanAndMergeOptions(updatedProp)
            paramMap[key] = updatedWithOptions
        }

        val updatedParams = params.map { paramMap[it.id] ?: it }.toMutableList()
        val updatedContainer = trigger.copy(params = updatedParams)
        val updatedFirstStage = model.stages.first().copy(
            containers = listOf(updatedContainer) +
                model.stages.first().containers.drop(1)
        )
        return model.copy(
            stages = listOf(updatedFirstStage) + model.stages.drop(1)
        )
    }

    private fun determineDefaultValue(value: String, type: BuildFormPropertyType): Any {
        // MULTIPLE 在 Model 里以逗号分隔字符串存储（与 TemplateInstanceUtil 一致），不要拆成 List
        return when (type) {
            BuildFormPropertyType.BOOLEAN -> value.toBoolean()
            else -> value
        }
    }

    private fun cleanAndMergeOptions(prop: BuildFormProperty): BuildFormProperty {
        if (prop.options.isNullOrEmpty()) return prop
        return PipelineUtils.cleanOptions(prop)
    }
}
