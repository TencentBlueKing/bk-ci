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
    fun getAllContextCase(): List<Map<String, String>> {
        val caseList = mutableListOf<Map<String, Any>>()
        caseList.addAll(calculateContextMatrix(strategy))

        // 先对json中的额外和排除做增删
        caseList.removeAll(exclude!!) // 排除特定的参数组合
        caseList.addAll(include!!) // 追加额外的参数组合

        return caseList.map { list ->
            list.map { map -> "${MATRIX_CONTEXT_KEY_PREFIX}${map.key}" to map.value.toString() }.toMap()
        }.toList().distinct()
    }

    /**
     * 根据[strategyMap]矩阵生成所有参数组合列表
     */
    private fun calculateContextMatrix(strategyMap: Map<String, List<Any>>?): List<Map<String, Any>> {
        if (strategyMap.isNullOrEmpty()) {
            return emptyList()
        }
        val caseList = mutableListOf<Map<String, String>>()
        val keyList = strategyMap.keys
        MatrixContextUtils.loopCartesianProduct(strategyMap.values.toList())
            .forEach { valueList ->
                val case = mutableMapOf<String, String>()
                keyList.forEachIndexed { index, key ->
                    case[key] = valueList[index].toString()
                }
                caseList.add(case)
            }
        return caseList
    }
}
