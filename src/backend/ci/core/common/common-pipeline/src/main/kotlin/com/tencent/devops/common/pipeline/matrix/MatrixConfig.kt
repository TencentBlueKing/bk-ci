package com.tencent.devops.common.pipeline.matrix

import com.tencent.devops.common.pipeline.utils.MatrixContextUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("矩阵的分裂计算配置")
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

        // 先对json中的额外和排除做增删
        exclude?.let { combinations.removeAll(exclude) } // 排除特定的参数组合

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
            combinations.forEach { case ->
                if (keyValueMatch(case, includeCase, matchKey)) {
                    // 将全匹配的额外参数直接追加到匹配的组合
                    case.putAll(includeCase)
                } else {
                    // 不能全匹配的额外参数作为一个新组合加入
                    caseToAdd.add(includeCase.toMutableMap())
                }
            }
        }
        combinations.addAll(caseToAdd)

        return combinations.map { list ->
            list.map { map -> "${MATRIX_CONTEXT_KEY_PREFIX}${map.key}" to map.value }.toMap()
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
