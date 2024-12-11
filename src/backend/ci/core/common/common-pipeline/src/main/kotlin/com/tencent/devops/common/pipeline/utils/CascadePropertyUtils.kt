package com.tencent.devops.common.pipeline.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.cascade.RepoRefCascadeParam.Companion.SELECTOR_KEY_BRANCH
import com.tencent.devops.common.pipeline.pojo.cascade.RepoRefCascadeParam.Companion.SELECTOR_KEY_REPO_NAME
import org.slf4j.LoggerFactory

object CascadePropertyUtils {
    fun getCascadeVariableKeyMap(key: String, type: BuildFormPropertyType) = when (type) {
        BuildFormPropertyType.REPO_REF -> {
            mapOf(
                SELECTOR_KEY_REPO_NAME to "$key.$SELECTOR_KEY_REPO_NAME",
                SELECTOR_KEY_BRANCH to "$key.$SELECTOR_KEY_BRANCH"
            )
        }

        else -> mapOf()
    }

    /**
     * 获取级联选择器的参数值的拼接规则
     * eg：xxx.repo-name,xxx.branch --> ${{xxx.repo-name}}@${{xxx.branch}}
     */
    private fun getCascadeVariableSubKey(key: String, type: BuildFormPropertyType) = when (type) {
        BuildFormPropertyType.REPO_REF -> {
            listOf(
                "$key.$SELECTOR_KEY_REPO_NAME",
                "$key.$SELECTOR_KEY_BRANCH"
            )
        }

        else -> listOf()
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
        BuildFormPropertyType.REPO_REF -> {
            mapOf(
                SELECTOR_KEY_REPO_NAME to "",
                SELECTOR_KEY_BRANCH to ""
            )
        }

        else -> mapOf()
    }

    fun supportCascadeParam(type: BuildFormPropertyType?) = type == BuildFormPropertyType.REPO_REF

    val logger = LoggerFactory.getLogger(CascadePropertyUtils::class.java)
}