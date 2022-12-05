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

package com.tencent.bkrepo.helm.listener.operation

import com.tencent.bkrepo.helm.pojo.chart.ChartOperationRequest
import com.tencent.bkrepo.helm.pojo.chart.ChartVersionDeleteRequest
import com.tencent.bkrepo.helm.pojo.metadata.HelmChartMetadata
import com.tencent.bkrepo.helm.pojo.metadata.HelmIndexYamlMetadata
import com.tencent.bkrepo.helm.service.impl.AbstractChartService
import java.util.SortedSet

class ChartDeleteOperation(
    private val request: ChartOperationRequest,
    chartService: AbstractChartService
) : AbstractChartOperation(request, chartService) {

    override fun handleEvent(helmIndexYamlMetadata: HelmIndexYamlMetadata) {
        logger.info("Prepare to delete metadata from index's metadata..")
        val deleteRequest = request as ChartVersionDeleteRequest
        with(deleteRequest) {
            val entries = helmIndexYamlMetadata.entries
            if (!entries.containsKey(name)) {
                logger.info("The metadata [$name] was not matched in the index file, return.")
                return
            }
            if (entries[name].orEmpty().none { it.version == version }) {
                logger.info(
                    "The metadata [$name] with version [$version] was not matched in the index file, return."
                )
                return
            }
            val chartMetadataSet = entries[name]!!
            if (chartMetadataSet.size == 1 && (version == chartMetadataSet.first().version)) {
                entries.remove(name)
            } else {
                updateIndexYaml(version, chartMetadataSet)
            }
            logger.info(
                "delete version: updated entries size: [${entries.size}], " +
                    "chart [$name] metadata size: [${chartMetadataSet.size}]"
            )
        }
    }

    private fun updateIndexYaml(version: String, chartMetadataSet: SortedSet<HelmChartMetadata>) {
        run stop@{
            chartMetadataSet.forEachIndexed { _, helmChartMetadata ->
                if (version == helmChartMetadata.version) {
                    chartMetadataSet.remove(helmChartMetadata)
                    return@stop
                }
            }
        }
    }
}
