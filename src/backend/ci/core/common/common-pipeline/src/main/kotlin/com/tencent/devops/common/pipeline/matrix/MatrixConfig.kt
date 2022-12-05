package com.tencent.devops.common.pipeline.matrix

import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.pipeline.utils.MatrixContextUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("矩阵的分裂计算配置")
@Suppress("ComplexMethod")
data class MatrixConfig(
    @ApiModelProperty("分裂策略", required = true)
    val strategy: Map<String, List<String>>?,
    @ApiModelProperty("额外的参数组合", required = true)
    val include: MutableList<Map<String, String>>?,
    @ApiModelProperty("排除的参数组合", required = false)
    val exclude: MutableList<Map<String, String>>?
) {

    companion object {
        const val MATRIX_CONTEXT_KEY_PREFIX = "matrix."
    }

    /**
     * 根据[strategy], [include], [exclude]矩阵参数计算最终参数组合列表
     */
    fun getAllCombinations(): List<Map<String, String>> {
        val combinations = mutableListOf<MutableMap<String, String>>()
        val (keyList, strategyCase) = calculateContextMatrix(strategy)
        combinations.addAll(strategyCase)

        // 将额外添加的参数在匹配的组合内进行追加
        val caseToAdd = mutableListOf<MutableMap<String, String>>()
        include?.forEach { includeCase ->
            // 如果strategy为空，直接添加include
            if (strategyCase.isEmpty()) {
                caseToAdd.add(includeCase.toMutableMap())
                return@forEach
            }
            // 筛选出所有与矩阵匹配的key
            val matchKey = includeCase.keys.filter { keyList.contains(it) }
            // 如果没有匹配的key则直接丢弃
            if (matchKey.isEmpty()) return@forEach
            var expanded = false
            val caseToAddTmp = mutableListOf<MutableMap<String, String>>()
            combinations.forEach { case ->
                if (keyValueMatch(case, includeCase, matchKey)) {
                    // 将全匹配的额外参数直接追加到匹配的组合
                    case.putAll(includeCase)
                    expanded = true
                } else {
                    // 不能全匹配的额外参数作为一个新组合加入
                    caseToAddTmp.add(includeCase.toMutableMap())
                }
            }
            if (!expanded) caseToAdd.addAll(caseToAddTmp)
        }
        combinations.addAll(caseToAdd)

        // 计算strategy和include后，再进行组合排除
        exclude?.let { combinations.removeAll(exclude) } // 排除特定的参数组合

        return combinations.map { contextCase ->
            // 临时方案：支持解析value中的一级对象访问
            val resultCase = mutableMapOf<String, String>()
            contextCase.forEach { (key, value) ->
                resultCase["${MATRIX_CONTEXT_KEY_PREFIX}$key"] = value
                kotlin.runCatching {
                    YamlUtil.to<Map<String, Any>>(value)
                }.getOrNull()?.forEach { (pair, _) ->
                    val split = pair.split('=')
                    if (split.size == 2) {
                        resultCase["${MATRIX_CONTEXT_KEY_PREFIX}$key.${split[0]}"] = split[1]
                    }
                }
            }
            resultCase
        }.toList().distinct()
    }

    /**
     * 根据[strategyMap]矩阵生成所有参数组合列表
     */
    private fun calculateContextMatrix(
        strategyMap: Map<String, List<Any>>?
    ): Pair<List<String>, List<MutableMap<String, String>>> {
        if (strategyMap.isNullOrEmpty()) {
            return Pair(emptyList(), emptyList())
        }
        val caseList = mutableListOf<MutableMap<String, String>>()
        val keyList = strategyMap.keys.toList()
        MatrixContextUtils.loopCartesianProduct(strategyMap.values.toList())
            .forEach { valueList ->
                val case = mutableMapOf<String, String>()
                keyList.forEachIndexed { index, key ->
                    case[key] = valueList[index].toString()
                }
                caseList.add(case)
            }
        return Pair(keyList, caseList)
    }

    /**
     * 对比[case]和[includeCase]中所有匹配key[matchKey]的值是否相同
     * 出现任意不同的情况则不是全匹配
     */
    private fun keyValueMatch(
        case: Map<String, String>,
        includeCase: Map<String, String>,
        matchKey: List<String>
    ): Boolean {
        matchKey.forEach { key ->
            if (case[key] != includeCase[key]) return false
        }
        return true
    }
}
