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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import io.swagger.annotations.ApiModelProperty
import org.yaml.snakeyaml.Yaml

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
    var runningCount: Int? = null
) {

    /**
     * 根据[strategyStr]生成对应的矩阵参数表
     */
    fun convertStrategy(): Map<String, List<String>> {
        val strategyMap = try {
            YamlUtil.to<Map<String, List<String>>>(strategyStr)
        } catch (e: Exception) {
            try {
                JsonUtil.to(strategyStr)
            } catch (e: Exception) {
                emptyMap()
            }
        }
        // TODO 存在json和yaml两种情况
        return strategyMap
    }

    /**
     * 根据[includeCaseStr]生成额外增加的参数组合
     */
    fun convertIncludeCase(): List<Map<String, String>> {
        if (includeCaseStr.isNullOrBlank()) {
            return emptyList()
        }
        val includeCaseList = try {
            YamlUtil.to<List<Map<String, String>>>(includeCaseStr)
        } catch (e: Exception) {
            emptyList()
        }
        // TODO yaml解析
        return includeCaseList
    }

    /**
     * 根据[excludeCaseStr]生成需要排除的参数组合
     */
    fun convertExcludeCase(): List<Map<String, String>> {
        if (excludeCaseStr.isNullOrBlank()) {
            return emptyList()
        }
        val excludeCaseList = try {
            YamlUtil.to<List<Map<String, String>>>(excludeCaseStr)
        } catch (e: Exception) {
            emptyList()
        }
        // TODO yaml解析
        return excludeCaseList
    }
}
