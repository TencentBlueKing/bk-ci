package com.tencent.devops.common.pipeline.matrix

import com.tencent.devops.common.pipeline.utils.MatrixContextUtils
import io.swagger.annotations.ApiModelProperty

data class MatrixConfig(
    @ApiModelProperty("分裂策略", required = true)
    var strategy: Map<String, List<String>>,
    @ApiModelProperty("额外的参数组合", required = true)
    val include: MutableList<Map<String, String>>,
    @ApiModelProperty("排除的参数组合", required = false)
    val exclude: MutableList<Map<String, String>>
) {

    companion object {
        const val MATRIX_CONTEXT_KEY_PREFIX = "matrix."
    }

    /**
     * 根据矩阵参数计算最终参数组合列表
     */
    fun getAllContextCase(): List<Map<String, String>> {
        val caseList = mutableListOf<Map<String, Any>>()
        caseList.addAll(MatrixContextUtils.calculateContextMatrix(strategy))

        // 先对json中的额外和排除做增删
        caseList.removeAll(exclude) // 排除特定的参数组合
        caseList.addAll(include) // 追加额外的参数组合

        return caseList.map { list ->
            list.map { map -> "${MATRIX_CONTEXT_KEY_PREFIX}${map.key}" to map.value.toString() }.toMap()
        }.toList().distinct()
    }
}
