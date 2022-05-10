/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.opdata.handler.impl

import com.tencent.bkrepo.common.api.util.HumanReadable
import com.tencent.bkrepo.opdata.constant.OPDATA_PROJECT_ID
import com.tencent.bkrepo.opdata.constant.OPDATA_REPO_NAME
import com.tencent.bkrepo.opdata.handler.QueryHandler
import com.tencent.bkrepo.opdata.pojo.NodeResult
import com.tencent.bkrepo.opdata.pojo.Target
import com.tencent.bkrepo.opdata.pojo.enums.Metrics
import com.tencent.bkrepo.opdata.repository.SizeDistributionMetricsRepository
import org.springframework.stereotype.Component

/**
 * 文件大小分布统计
 */
@Component
class NodeSizeDistributionHandler(
    private val sizeDistributionMetricsRepository: SizeDistributionMetricsRepository
) : QueryHandler {

    override val metric: Metrics get() = Metrics.NODESIZEDISTRIBUTION

    @Suppress("UNCHECKED_CAST")
    override fun handle(target: Target, result: MutableList<Any>) {
        val resultList = if (target.data.toString().isBlank()) {
            sizeDistributionMetricsRepository.findAll()
        } else {
            val data = target.data as Map<String, Any>
            val projectId = data[OPDATA_PROJECT_ID] as String?
            val repoName = data[OPDATA_REPO_NAME] as String?
            if (projectId.isNullOrBlank()) {
                sizeDistributionMetricsRepository.findAll()
            } else if (repoName.isNullOrBlank()) {
                sizeDistributionMetricsRepository.findByProjectId(projectId)
            } else {
                sizeDistributionMetricsRepository.findByProjectIdAndRepoName(projectId, repoName)
            }
        }
        val resultMap = mutableMapOf<String, Long>()
        resultList.forEach {
            val keys = it.sizeDistribution.keys
            keys.forEach { key ->
                resultMap[key] = resultMap[key]?.plus(it.sizeDistribution[key]!!) ?: it.sizeDistribution[key]!!
            }
        }
        val results = resultMap.toList().map { Pair(it.first.toLong(), it.second) }.sortedBy { it.first }
        for (i in results.indices) {
            val size = if (i + 1 < results.size) {
                "${HumanReadable.size(results[i].first)} - ${HumanReadable.size(results[i + 1].first)}"
            } else {
                "> ${HumanReadable.size(results[i].first)}"
            }
            val data = listOf(results[i].second, System.currentTimeMillis())
            val element = listOf(data)
            result.add(NodeResult(size, element))
        }
    }
}
