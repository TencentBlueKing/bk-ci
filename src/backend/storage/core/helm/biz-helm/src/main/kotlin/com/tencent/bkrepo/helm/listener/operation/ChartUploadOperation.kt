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

import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.helm.pojo.chart.ChartOperationRequest
import com.tencent.bkrepo.helm.pojo.chart.ChartUploadRequest
import com.tencent.bkrepo.helm.pojo.metadata.HelmChartMetadata
import com.tencent.bkrepo.helm.pojo.metadata.HelmIndexYamlMetadata
import com.tencent.bkrepo.helm.service.impl.AbstractChartService
import com.tencent.bkrepo.helm.utils.ChartParserUtil
import com.tencent.bkrepo.repository.pojo.node.NodeDetail

class ChartUploadOperation(
    private val request: ChartOperationRequest,
    private val helmChartMetadata: HelmChartMetadata,
    private val domain: String,
    private val nodeDetail: NodeDetail,
    chartService: AbstractChartService
) : AbstractChartOperation(request, chartService) {

    override fun handleEvent(helmIndexYamlMetadata: HelmIndexYamlMetadata) {
        logger.info("Prepare to add metadata to index's metadata..")
        val uploadRequest = request as ChartUploadRequest
        with(uploadRequest) {
            logger.info("Adding chart info to index.yaml...")
            val chartName = helmChartMetadata.name
            val chartVersion = helmChartMetadata.version
            helmChartMetadata.urls = listOf(
                UrlFormatter.format(
                    domain, "$projectId/$repoName/charts/$chartName-$chartVersion.tgz"
                )
            )
            helmChartMetadata.created = AbstractChartService.convertDateTime(nodeDetail.createdDate)
            helmChartMetadata.digest = nodeDetail.sha256
            ChartParserUtil.addIndexEntries(helmIndexYamlMetadata, helmChartMetadata)
        }
    }
}
