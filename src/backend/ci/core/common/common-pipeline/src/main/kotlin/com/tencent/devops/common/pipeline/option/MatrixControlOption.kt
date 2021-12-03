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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import io.swagger.annotations.ApiModelProperty

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
    val maxConcurrency: Int? = null,
    @ApiModelProperty("矩阵组的总数量", required = false)
    var totalCount: Int? = null,
    @ApiModelProperty("正在运行的数量", required = false)
    var runningCount: Int? = null,
) {

    /**
     * 根据[strategyStr], [includeCaseStr], [excludeCaseStr]计算后得到矩阵参数表
     */
    fun getAllContextCase(): List<Map<String, String>> {
        val matrixParamMap = mutableListOf<Map<String, String>>()
        matrixParamMap.addAll(calculateContextMatrix(convertStrategy()))
        matrixParamMap.addAll(convertCase(includeCaseStr)) // 追加额外的参数组合
        matrixParamMap.removeAll(convertCase(excludeCaseStr)) // 排除特定的参数组合
        return matrixParamMap
    }

    /**
     * 根据[strategyStr]生成对应的矩阵参数表
     */
    private fun calculateContextMatrix(strategyMap: Map<String, List<String>>): List<Map<String, String>> {
        val caseList = mutableListOf<Map<String, String>>()
        val keyList = strategyMap.keys
        cartesianProduct(strategyMap.values.toList())
            .forEach { valueList ->
                val case = mutableMapOf<String, String>()
                keyList.forEachIndexed { index, key ->
                    case[key] = valueList[index].toString()
                }
                caseList.add(case)
            }
        return caseList
    }

    /**
     * 根据[strategyStr]生成对应的矩阵参数表
     */
    private fun convertStrategy(): Map<String, List<String>> {
        val strategyMap = try {
            YamlUtil.to<Map<String, List<String>>>(strategyStr)
        } catch (ignore: Throwable) {
            try {
                JsonUtil.to(strategyStr)
            } catch (ignore: Throwable) {
                emptyMap()
            }
        }
        // TODO 存在json和yaml两种情况
        return strategyMap
    }

    /**
     * 传入[includeCaseStr]或[excludeCaseStr]的获得组合数组
     */
    private fun convertCase(str: String?): List<Map<String, String>> {
        if (str.isNullOrBlank()) {
            return emptyList()
        }
        val includeCaseList = try {
            YamlUtil.to<List<Map<String, String>>>(str)
        } catch (ignore: Throwable) {
            emptyList()
        }
        return includeCaseList
    }

    /**
     * Kotlin实现的笛卡尔乘积算法，[input]需要包含两个以上元素
     */
    private fun cartesianProduct(input: List<List<Any>>): List<List<Any>> =
        input.fold(listOf(listOf<Any>())) { acc, set ->
            acc.flatMap { list -> set.map { element -> list + element } }
        }.toList()
}
