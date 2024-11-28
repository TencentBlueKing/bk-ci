package com.tencent.devops.common.pipeline.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.cascade.RepoRefCascadeParam
import org.slf4j.LoggerFactory

object CascadePropertyUtils {
    fun getCascadeVariableKeyMap(key: String, type: BuildFormPropertyType) = when (type) {
        BuildFormPropertyType.REPO_REF -> RepoRefCascadeParam.variableKeyMap(key)
        else -> mapOf()
    }

    /**
     * 解析级联选择器默认值，当解析默认值失败时，使用默认值
     */
    fun parseDefaultValue(key: String, defaultValue: Any, type: BuildFormPropertyType?) = try {
        if (defaultValue is String) {
            JsonUtil.to(
                json = defaultValue,
                typeReference = object : TypeReference<Map<String, String>>() {}
            )
        } else {
            defaultValue as Map<String, String>
        }
    } catch (ignored: Exception) {
        logger.warn("parse repo ref error, key: $key, defaultValue: $defaultValue")
        getDefaultValue(type)
    }

    private fun getDefaultValue(type: BuildFormPropertyType?) = when (type) {
        BuildFormPropertyType.REPO_REF -> RepoRefCascadeParam.defaultValue()
        else -> mapOf()
    }

    fun supportCascadeParam(type: BuildFormPropertyType?) = type == BuildFormPropertyType.REPO_REF

    val logger = LoggerFactory.getLogger(CascadePropertyUtils::class.java)
}