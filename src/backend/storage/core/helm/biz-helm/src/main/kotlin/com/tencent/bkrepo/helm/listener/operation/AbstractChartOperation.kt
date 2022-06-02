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
import com.tencent.bkrepo.helm.pojo.metadata.HelmIndexYamlMetadata
import com.tencent.bkrepo.helm.service.impl.AbstractChartService
import com.tencent.bkrepo.helm.utils.HelmUtils
import com.tencent.bkrepo.helm.utils.ObjectBuilderUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.StopWatch

abstract class AbstractChartOperation(
    private val request: ChartOperationRequest,
    private val chartService: AbstractChartService
) : Runnable {
    override fun run() {
        with(request) {
            val stopWatch = StopWatch(
                "Handling event for refreshing index.yaml " +
                    "in repo [$projectId/$repoName] by User [$operator]"
            )
            stopWatch.start()
            chartService.lockAction(projectId, repoName) { handleOperation(this) }
            stopWatch.stop()
            logger.info(
                "Total cost for refreshing index.yaml" +
                    "in repo [$projectId/$repoName] by User [$operator] is: ${stopWatch.totalTimeSeconds}s"
            )
        }
    }

    /**
     * 处理对应的chart操作用于更新index.yaml文件
     */
    private fun handleOperation(request: ChartOperationRequest) {
        with(request) {
            try {
                val stopWatch = StopWatch(
                    "getOriginalIndexYamlFile for refreshing index.yaml " +
                        "in repo [$projectId/$repoName] by User [$operator]"
                )
                stopWatch.start()
                val originalIndexYamlMetadata =
                    if (!chartService.exist(projectId, repoName, HelmUtils.getIndexCacheYamlFullPath())) {
                        HelmUtils.initIndexYamlMetadata()
                    } else {
                        chartService.getOriginalIndexYaml(projectId, repoName)
                    }
                stopWatch.stop()
                logger.info(
                    "query index.yaml file metadata " +
                        "in repo [$projectId/$repoName] by User [$operator] cost: ${stopWatch.totalTimeSeconds}s"
                )
                handleEvent(originalIndexYamlMetadata)
                logger.info("index.yaml in repo [$projectId/$repoName] is ready to upload...")
                val (artifactFile, nodeCreateRequest) = ObjectBuilderUtil.buildFileAndNodeCreateRequest(
                    originalIndexYamlMetadata, this
                )
                chartService.uploadIndexYamlMetadata(artifactFile, nodeCreateRequest)
                logger.info(
                    "Index.yaml has been refreshed by User [$operator] " +
                        "in repo [$projectId/$repoName] !"
                )
            } catch (e: Exception) {
                logger.error(
                    "Error [${e.message}] occurred while refreshing index.yaml by" +
                        " User [$operator] in repo [$projectId/$repoName] !"
                )
                throw e
            }
        }
    }

    /**
     * 处理对应的事件用于更新index.yaml中的meta data
     */
    open fun handleEvent(helmIndexYamlMetadata: HelmIndexYamlMetadata) {}

    companion object {
        val logger: Logger = LoggerFactory.getLogger(AbstractChartOperation::class.java)
    }
}
