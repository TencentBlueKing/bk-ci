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

package com.tencent.bkrepo.helm.listener

import com.tencent.bkrepo.helm.exception.HelmException
import com.tencent.bkrepo.helm.listener.event.ChartDeleteEvent
import com.tencent.bkrepo.helm.listener.event.ChartVersionDeleteEvent
import com.tencent.bkrepo.helm.utils.HelmUtils
import com.tencent.bkrepo.repository.api.NodeClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ChartEventListener(nodeClient: NodeClient) : AbstractEventListener(nodeClient) {

    /**
     * 删除chart版本，更新index.yaml文件
     */
    @Synchronized
    @EventListener(ChartVersionDeleteEvent::class)
    fun handle(event: ChartVersionDeleteEvent) {
        // 如果index.yaml文件不存在，说明还没有初始化该文件，return
        // 如果index.yaml文件存在，则进行更新
        with(event.request) {
            try {
                if (!exist(projectId, repoName, HelmUtils.getIndexYamlFullPath())) {
                    logger.warn("Index yaml file is not initialized, return.")
                    return
                }
                val originalIndexYamlMetadata = getOriginalIndexYaml()
                originalIndexYamlMetadata.entries.let {
                    val chartMetadataSet =
                        it[name] ?: throw HelmException("index.yaml file for chart [$name] not found.")
                    if (chartMetadataSet.size == 1 && (version == chartMetadataSet.first().version)) {
                        it.remove(name)
                    } else {
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
                uploadIndexYamlMetadata(originalIndexYamlMetadata)
                logger.info(
                    "User [$operator] fresh index.yaml for delete chart [$name], version [$version] " +
                        "in repo [$projectId/$repoName] success!"
                )
            } catch (exception: TypeCastException) {
                logger.error("User [$operator] fresh index.yaml for delete chart [$name], version [$version] " +
                    "in repo [$projectId/$repoName] failed, message: $exception")
                throw exception
            }
        }
    }

    /**
     * 删除chart版本，更新index.yaml文件
     */
    @Synchronized
    @EventListener(ChartDeleteEvent::class)
    fun handle(event: ChartDeleteEvent) {
        with(event.request) {
            try {
                if (!exist(projectId, repoName, HelmUtils.getIndexYamlFullPath())) {
                    logger.warn("Index yaml file is not initialized, return.")
                    return
                }
                val originalIndexYamlMetadata = getOriginalIndexYaml()
                originalIndexYamlMetadata.entries.remove(name)
                uploadIndexYamlMetadata(originalIndexYamlMetadata)
                logger.info(
                    "User [$operator] fresh index.yaml for delete chart [$name] " +
                        "in repo [$projectId/$repoName] success!"
                )
            } catch (exception: TypeCastException) {
                logger.error(
                    "User [$operator] fresh index.yaml for delete chart [$name] " +
                        "in repo [$projectId/$repoName] failed, message: $exception"
                )
                throw exception
            }
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ChartEventListener::class.java)
    }
}
