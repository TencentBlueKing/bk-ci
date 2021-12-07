/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.pipeline.option

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ReplacementUtils
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.pipeline.info.MatrixDispatchInfo
import com.tencent.devops.common.pipeline.utils.MatrixContextUtils
import com.tencent.devops.common.pipeline.pojo.MatrixConvert
import io.swagger.annotations.ApiModelProperty
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

/**
 *  构建矩阵配置项
 */
data class MatrixControlOption(
    @ApiModelProperty("分裂策略（支持变量、Json、参数映射表）", required = true)
    val strategyStr: String, // Map<String, List<String>>
    @ApiModelProperty("额外的参数组合（变量名到特殊值映射的数组）", required = false)
    val includeCaseStr: String? = null, // List<Map<String, String>>
    @ApiModelProperty("排除的参数组合（变量名到特殊值映射的数组）", required = false)
    val excludeCaseStr: String? = null, // List<Map<String, String>>
    @ApiModelProperty("是否启用容器失败快速终止整个矩阵", required = false)
    val fastKill: Boolean? = false,
    @ApiModelProperty("Job运行的最大并发量", required = false)
    val maxConcurrency: Int? = 20,
    @ApiModelProperty("自定义调度类型（用于生成DispatchType的任意对象）", required = false)
    var customDispatchInfo: MatrixDispatchInfo? = null, // DispatchTypeParser的传入和解析保持一致即可
    @ApiModelProperty("矩阵组的总数量", required = false)
    var totalCount: Int? = null,
    @ApiModelProperty("完成执行的数量", required = false)
    var finishCount: Int? = null
) {

    companion object {
        private val MATRIX_JSON_KEY_PATTERN = Pattern.compile("^(fromJSON\\()([^(^)]+)[\\)]\$")
        private val logger = LoggerFactory.getLogger(MatrixControlOption::class.java)
        const val MATRIX_CONTEXT_KEY_PREFIX = "matrix."
        const val MATRIX_CASE_MAX_COUNT = 256
    }

    /**
     * 根据[strategyStr], [includeCaseStr], [excludeCaseStr]计算后得到矩阵参数表
     */
    fun getAllContextCase(buildContext: Map<String, String>): List<Map<String, String>> {
        val caseList = mutableListOf<Map<String, Any>>()
        try {
            // 由于yaml和json结构不同，就不放在同一函数进行解析了
            caseList.addAll(calculateContextMatrix(convertStrategyYaml(buildContext)))
        } catch (ignore: Throwable) {
            logger.warn("convert Strategy from Yaml error. try parse with JSON. Error message: ${ignore.message}")
            caseList.addAll(convertStrategyJson(buildContext))
        }

        // #4518 先排除再追加
        caseList.removeAll(convertCase(EnvUtils.parseEnv(excludeCaseStr, buildContext))) // 排除特定的参数组合
        caseList.addAll(convertCase(EnvUtils.parseEnv(includeCaseStr, buildContext))) // 追加额外的参数组合

        return caseList.map { list ->
            list.map { map -> "$MATRIX_CONTEXT_KEY_PREFIX${map.key}" to map.value.toString() }.toMap()
        }.toList().distinctBy { it }
    }

    /**
     * 根据[strategyStr]生成对应的矩阵参数表
     */
    private fun calculateContextMatrix(strategyMap: Map<String, List<Any>>?): List<Map<String, Any>> {
        if (strategyMap.isNullOrEmpty()) {
            return emptyList()
        }
        val caseList = mutableListOf<Map<String, Any>>()
        val keyList = strategyMap.keys
        MatrixContextUtils.loopCartesianProduct(strategyMap.values.toList())
            .forEach { valueList ->
                val case = mutableMapOf<String, Any>()
                keyList.forEachIndexed { index, key ->
                    case[key] = valueList[index]
                }
                caseList.add(case)
            }
        return caseList
    }

    /**
     * 根据[strategyStr]生成对应的矩阵参数表
     */
    private fun convertStrategyYaml(buildContext: Map<String, String>): Map<String, List<Any>> {
        val contextStr = EnvUtils.parseEnv(strategyStr, buildContext)
        return YamlUtil.to<Map<String, List<Any>>>(contextStr)
    }

    /**
     * 根据[strategyStr]生成对应的矩阵参数表
     */
    private fun convertStrategyJson(buildContext: Map<String, String>): List<Map<String, Any>> {
        // 替换上下文 要考虑带fromJSON()的写法
        val contextStr = ReplacementUtils.replace(
            command = strategyStr,
            replacement = object : ReplacementUtils.KeyReplacement {
                override fun getReplacement(key: String): String? {
                    // 匹配fromJSON()
                    val matcher = MATRIX_JSON_KEY_PATTERN.matcher(key)
                    if (matcher.find()) {
                        return buildContext[matcher.group(2)]
                    }
                    return buildContext[key]
                }
            }
        )
        val matrixParamMap = mutableListOf<Map<String, Any>>()
        try {
            val jsonMap = JsonUtil.to(contextStr, MatrixConvert::class.java)
            matrixParamMap.addAll(calculateContextMatrix(jsonMap.strategy))
            matrixParamMap.removeAll(jsonMap.exclude ?: emptyList()) // 排除特定的参数组合
            matrixParamMap.addAll(jsonMap.include ?: emptyList()) // 追加额外的参数组合
        } catch (ignore: Throwable) {
            logger.error("convert Strategy from Json error : ${ignore.message}", ignore)
        }
        return matrixParamMap
    }

    /**
     * 传入[includeCaseStr]或[excludeCaseStr]的获得组合数组
     */
    private fun convertCase(str: String?): List<Map<String, Any>> {
        if (str.isNullOrBlank()) {
            return emptyList()
        }
        val includeCaseList = try {
            YamlUtil.to<List<Map<String, Any>>>(str)
        } catch (ignore: Throwable) {
            emptyList()
        }
        return includeCaseList
    }
}
