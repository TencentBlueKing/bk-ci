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

import com.tencent.bkrepo.opdata.constant.OPDATA_PROJECT_ID
import com.tencent.bkrepo.opdata.constant.OPDATA_REPO_NAME
import com.tencent.bkrepo.opdata.handler.QueryHandler
import com.tencent.bkrepo.opdata.model.FileExtensionMetricsModel
import com.tencent.bkrepo.opdata.model.TFileExtensionMetrics
import com.tencent.bkrepo.opdata.pojo.NodeResult
import com.tencent.bkrepo.opdata.pojo.Target
import com.tencent.bkrepo.opdata.pojo.enums.Metrics
import com.tencent.bkrepo.opdata.pojo.enums.StatMetrics
import org.springframework.stereotype.Component

/**
 * 依据文件后缀名的文件数量、大小统计
 */
@Component
class FileExtensionHandler(
    private val fileExtensionMetricsModel: FileExtensionMetricsModel
) : QueryHandler {

    override val metric: Metrics get() = Metrics.FILEEXTENSION

    @Suppress("UNCHECKED_CAST")
    override fun handle(target: Target, result: MutableList<Any>) {
        val reqData = if (target.data.toString().isBlank()) null else target.data as Map<String, Any>
        val projectId = reqData?.get(OPDATA_PROJECT_ID) as String?
        val repoName = reqData?.get(OPDATA_REPO_NAME) as String?
        val metric = StatMetrics.valueOf(reqData?.get(STAT_METRICS) as? String ?: StatMetrics.NUM.name)
        val resultList = if (projectId.isNullOrBlank()) {
            fileExtensionMetricsModel.getFileExtensionMetrics(metric)
        } else if (repoName.isNullOrBlank()) {
            fileExtensionMetricsModel.getProjFileExtensionMetrics(projectId, metric)
        } else {
            fileExtensionMetricsModel.getRepoFileExtensionMetrics(projectId, repoName, metric)
        }
        resultList.forEach {
            val data = listOf(it[metric.name.toLowerCase()] as Long, System.currentTimeMillis())
            val element = listOf(data)
            result.add(NodeResult(it[TFileExtensionMetrics::extension.name] as String, element))
        }
    }

    companion object {
        private const val STAT_METRICS = "metrics"
    }
}
