package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.cascade.RepoRefCascadeParam.Companion.SELECTOR_KEY_BRANCH
import com.tencent.devops.common.pipeline.pojo.cascade.RepoRefCascadeParam.Companion.SELECTOR_KEY_REPO_NAME

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

    fun supportCascadeParam(type: BuildFormPropertyType?) = type == BuildFormPropertyType.REPO_REF
}