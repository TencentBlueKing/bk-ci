package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
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

    /**
     * 处理级联选择框参数
     * 将xxx.repo-name 和 xxx.branch 合并为 xxx=repo-name@branch
     */
    fun mergeCascadeParams(parameters: List<BuildParameters>): List<BuildParameters> {
        val repoRefParams =
            parameters.filter { supportCascadeParam(it.valueType) }
                .groupBy { it.relKey }
                .mapValues { (relKey, params) ->
                    // xxx.repo-name to buildParam
                    // xxx.branch to buildParam
                    val associate = params.associateBy { param -> param.key }
                    // 拼接参数
                    val value = getCascadeVariableSubKey(
                        key = relKey ?: "",
                        type = BuildFormPropertyType.REPO_REF
                    ).joinToString(separator = "@") { subKey -> associate[subKey]?.value.toString() ?: "" }
                    BuildParameters(
                        key = relKey ?: "",
                        value = value,
                        valueType = BuildFormPropertyType.REPO_REF,
                        desc = params.first().desc,
                        readOnly = params.first().readOnly,
                        relKey = relKey
                    )
                }.map { it.value }
        val list = parameters.filter { it.valueType != BuildFormPropertyType.REPO_REF }
            .toMutableList()
        list.addAll(repoRefParams)
        return list
    }

    /**
     * 获取上一次构建的级联选择参数
     */
    fun getLatestCascadeParamsValue(
        param: Map<String, Any>,
        type: BuildFormPropertyType,
        relKey: String
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        getCascadeVariableKeyMap(relKey, type).forEach { (subKey, paramKey) ->
            result[subKey] = param[paramKey].toString()
        }
        return result
    }
}