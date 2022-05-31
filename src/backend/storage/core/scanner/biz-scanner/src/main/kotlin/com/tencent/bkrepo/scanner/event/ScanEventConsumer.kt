/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.event

import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.scanner.pojo.request.MatchPlanSingleScanRequest
import com.tencent.bkrepo.scanner.service.ScanService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.util.function.Consumer

/**
 * 构件事件消费者，用于触发制品更新扫描
 * 制品有新的推送时，筛选已开启自动扫描的方案进行扫描
 * 对应binding name为artifactEvent-in-0
 */
@Component("artifactEvent")
class ScanEventConsumer(
    private val scanService: ScanService
) : Consumer<ArtifactEvent> {

    /**
     * 允许接收的事件类型
     */
    private val acceptTypes = setOf(
        EventType.NODE_CREATED,
        EventType.VERSION_CREATED,
        EventType.VERSION_UPDATED
    )

    override fun accept(event: ArtifactEvent) {
        with(event) {
            if (!acceptTypes.contains(type)) {
                return
            }

            logger.info("event.resourceKey[${event.resourceKey}]")
            val request = when (type) {
                EventType.NODE_CREATED -> {//GENERIC仓库
                    val artifactName = File(resourceKey).name
                    //只支持ipa/apk类型包
                    if (!artifactName.endsWith(".apk") && !artifactName.endsWith(".ipa")) {
                        return
                    }
                    MatchPlanSingleScanRequest(
                        projectId = projectId,
                        repoName = repoName,
                        fullPath = resourceKey
                    )
                }
                EventType.VERSION_CREATED, EventType.VERSION_UPDATED -> {
                    if ((data["packageType"] as? String) != PackageType.MAVEN.name) {
                        return
                    }
                    MatchPlanSingleScanRequest(
                        projectId = projectId,
                        repoName = repoName,
                        packageName = (data["packageName"] as? String).orEmpty(),
                        packageKey = data["packageKey"].toString(),
                        version = data["packageVersion"].toString()
                    )
                }
                else -> throw UnsupportedOperationException()
            }
            scanService.matchPlanScan(request)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScanEventConsumer::class.java)
    }
}
