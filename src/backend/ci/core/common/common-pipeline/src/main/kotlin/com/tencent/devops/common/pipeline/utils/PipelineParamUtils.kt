package com.tencent.devops.common.pipeline.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import org.slf4j.LoggerFactory

private const val CONTEXT_PREFIX = "variables."

/**
 * 流水线变量工具类
 */
object PipelineParamUtils {
    /**
     * 解析默认值
     */
    fun parseDefaultValue(param: BuildFormProperty): Map<String, String> {
        val startParams = mutableMapOf<String, String>()
        val paramKey = param.id
        val paramDefaultValue = param.defaultValue
        val paramType = param.type

        when {
            // 级联参数
            CascadePropertyUtils.supportCascadeParam(paramType) -> {
                CascadePropertyUtils.parseDefaultValue(
                    key = paramKey,
                    defaultValue = paramDefaultValue,
                    type = paramType
                ).forEach {
                    startParams["$paramKey.${it.key}"] = it.value
                }
            }

            // 自定义参数
            paramType == BuildFormPropertyType.FORM_LIST -> {
                try {
                    val customParams = paramDefaultValue as List<Map<String, String>>
                    customParams.forEachIndexed { index, map ->
                        map.forEach { (key, value) ->
                            startParams["$paramKey.$index.$key"] = value
                        }
                    }
                } catch (ignored: Exception) {
                    logger.warn(
                        "parse custom param error, key: $paramKey, " +
                                "defaultValue: ${JsonUtil.toJson(paramDefaultValue, false)}"
                    )
                }
            }

            // 其他参数
            else -> {
                startParams[paramKey] = paramDefaultValue.toString()
            }
        }

        return startParams
    }

    /**
     * 获取启动参数列表
     */
    fun getStartParamList(
        param: BuildParameters,
        originStartContexts: HashMap<String, BuildParameters>
    ) = when {
        // 级联参数
        CascadePropertyUtils.supportCascadeParam(param.valueType) -> {
            fillCascadeParam(param, originStartContexts)
        }

        // 自定义复杂参数
        param.valueType == BuildFormPropertyType.FORM_LIST -> {
            fillCustomParam(param, originStartContexts)
        }

        else -> {
            fillContextPrefix(param, originStartContexts)
            listOf(param)
        }
    }

    /**
     * 填充级联参数
     * 示例：xxx = {"repo-name": "xxx/xxx","branch":"master"}
     * 生成：xxx.repo-name = xxx/xxx, xxx.branch = master
     */
    fun fillCascadeParam(
        param: BuildParameters,
        originStartContexts: HashMap<String, BuildParameters>
    ): List<BuildParameters> {
        val key = param.key
        val paramValue = CascadePropertyUtils.parseDefaultValue(key, param.value, param.valueType)
        val allParams = mutableListOf(param)
        CascadePropertyUtils.getCascadeVariableKeyMap(
            key = key,
            type = param.valueType!!
        ).forEach { (subKey, paramKey) ->
            allParams.add(param.copy(key = paramKey, value = paramValue[subKey] ?: ""))
        }

        allParams.forEach { fillContextPrefix(it, originStartContexts) }
        return allParams
    }

    /**
     * 填充自定义参数
     * 示例：xxx = {"name": "xxx","url":"yyy","branch":"master"}
     * 生成：xxx.name = xxx, xxx.url = yyy, xxx.branch = master
     */
    private fun fillCustomParam(
        param: BuildParameters,
        originStartContexts: HashMap<String, BuildParameters>
    ): List<BuildParameters> {
        val key = param.key
        val value = param.value
        val paramValue = try {
            when (value) {
                is String -> {
                    JsonUtil.to(value, object : TypeReference<List<Map<String, String>>>() {})
                }
                else -> {
                    value as List<Map<String, String>>
                }
            }
        } catch (ignored: Exception) {
            logger.warn("parse custom param error, key: $key, defaultValue: $value")
            listOf()
        }
        val allParams = mutableListOf(param)
        // 下级参数，填充住参数名前缀
        paramValue.forEachIndexed { index, map ->
            map.forEach { (subKey, subValue) ->
                allParams.add(param.copy(key = "$key.$index.$subKey", value = subValue ?: ""))
            }
        }
        // 添加variables前缀
        allParams.forEach { fillContextPrefix(it, originStartContexts) }
        return allParams
    }

    /**
     * 填充上下文前缀
     */
    private fun fillContextPrefix(
        param: BuildParameters,
        originStartContexts: HashMap<String, BuildParameters>
    ) {
        with(param) {
            if (key.startsWith(CONTEXT_PREFIX)) {
                originStartContexts[key] = param
            } else {
                val ctxKey = CONTEXT_PREFIX + key
                originStartContexts[ctxKey] = param.copy(key = ctxKey)
            }
        }
    }

    private val logger = LoggerFactory.getLogger(PipelineParamUtils::class.java)
}
