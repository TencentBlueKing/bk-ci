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

import com.tencent.bkrepo.common.api.constant.CharPool
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.event.packages.VersionCreatedEvent
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.query.matcher.RuleMatcher
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.scanner.configuration.ScannerProperties
import com.tencent.bkrepo.scanner.dao.ProjectScanConfigurationDao
import com.tencent.bkrepo.scanner.dao.ScanPlanDao
import com.tencent.bkrepo.scanner.pojo.ScanTriggerType
import com.tencent.bkrepo.scanner.pojo.request.ScanRequest
import com.tencent.bkrepo.scanner.pojo.rule.RuleArtifact
import com.tencent.bkrepo.scanner.service.ScanService
import com.tencent.bkrepo.scanner.utils.RuleConverter
import org.slf4j.LoggerFactory
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.util.function.Consumer

/**
 * 构件事件消费者，用于触发制品更新扫描
 * 制品有新的推送时，筛选已开启自动扫描的方案进行扫描
 * 对应binding name为artifactEvent-in-0
 */
@Component("artifactEvent")
class ScanEventConsumer(
    private val scanService: ScanService,
    private val scanPlanDao: ScanPlanDao,
    private val projectScanConfigurationDao: ProjectScanConfigurationDao,
    private val executor: ThreadPoolTaskExecutor,
    private val scannerProperties: ScannerProperties
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
        if (!acceptTypes.contains(event.type)) {
            return
        }

        executor.execute {
            when (event.type) {
                EventType.NODE_CREATED -> scanOnNodeCreatedEvent(event)
                EventType.VERSION_CREATED, EventType.VERSION_UPDATED -> scanOnVersionCreated(event)
                else -> throw UnsupportedOperationException()
            }
        }
    }

    /**
     * 当新制品上传时执行扫描
     *
     * @param event 新制品上传事件，NodeCreatedEvent只有Generic仓库会产生
     *
     * @return 是否有扫描任务创建
     */
    private fun scanOnNodeCreatedEvent(event: ArtifactEvent): Boolean {
        if (!supportFileNameExtension(event.resourceKey)) {
            return false
        }
        logger.info("receive event resourceKey[${event.resourceKey}]")

        var hasScanTask = false
        with(event) {
            scanPlanDao
                .findByProjectIdAndRepoName(projectId, repoName, RepositoryType.GENERIC.name)
                .filter { match(event, it.rule.readJsonString()) }
                .forEach {
                    val request = ScanRequest(
                        planId = it.id!!,
                        rule = RuleConverter.convert(projectId, repoName, resourceKey)
                    )
                    scanService.scan(request, ScanTriggerType.ON_NEW_ARTIFACT)
                    hasScanTask = true
                }
        }

        if (!hasScanTask) {
            scanIfHasProjectConfiguration(event)
        }

        return hasScanTask
    }

    private fun supportFileNameExtension(fullPath: String): Boolean {
        val fileNameExtension = fullPath.substringAfterLast('.', "")
        return fileNameExtension in scannerProperties.supportFileNameExt
    }

    /**
     * 当package有新版本时执行扫描
     *
     * @param event package新版本创建事件
     *
     * @return 是否有扫描任务创建
     */
    private fun scanOnVersionCreated(event: ArtifactEvent): Boolean {
        var hasScanTask = false

        with(event) {
            if (data[VersionCreatedEvent::packageType.name] != PackageType.MAVEN.name) {
                return false
            }
            logger.info("receive event resourceKey[${event.resourceKey}]")

            scanPlanDao
                .findByProjectIdAndRepoName(projectId, repoName, PackageType.MAVEN.name)
                .filter { match(event, it.rule.readJsonString()) }
                .forEach {
                    val packageKey = data[VersionCreatedEvent::packageKey.name] as String
                    val packageVersion = data[VersionCreatedEvent::packageVersion.name] as String
                    val request = ScanRequest(
                        planId = it.id!!,
                        rule = RuleConverter.convert(projectId, repoName, packageKey, packageVersion)
                    )
                    scanService.scan(request, ScanTriggerType.ON_NEW_ARTIFACT)
                    hasScanTask = true
                }
        }

        if (!hasScanTask) {
            scanIfHasProjectConfiguration(event)
        }

        return hasScanTask
    }

    /**
     * 执行系统层面设置的自动扫描
     */
    private fun scanIfHasProjectConfiguration(event: ArtifactEvent) {
        with(event) {
            val projectScanConfiguration = projectScanConfigurationDao.findByProjectId(event.projectId) ?: return
            for (entry in projectScanConfiguration.autoScanConfiguration.entries) {
                val scanner = entry.key
                val configuration = entry.value

                if (configuration.autoScanRepoNames.isNotEmpty() && repoName !in configuration.autoScanRepoNames
                    || !match(event, configuration.autoScanMatchRule)) {
                    continue
                }

                val rule = if (event.type == EventType.NODE_CREATED) {
                    RuleConverter.convert(projectId, repoName, resourceKey)
                } else {
                    val packageKey = data[VersionCreatedEvent::packageKey.name] as String
                    val packageVersion = data[VersionCreatedEvent::packageVersion.name] as String
                    RuleConverter.convert(projectId, repoName, packageKey, packageVersion)
                }
                val request = ScanRequest(scanner = scanner, rule = rule)
                scanService.scan(request, ScanTriggerType.ON_NEW_ARTIFACT)
            }
        }
    }

    /**
     * 判断制品是否匹配规则
     *
     * @return true 匹配规则或者rule为null， false 不匹配
     */
    private fun match(event: ArtifactEvent, rule: Rule?): Boolean {
        if (rule == null) {
            return true
        }

        with(event) {
            if (event.type == EventType.NODE_CREATED) {
                val valuesToMatch = mapOf(
                    NodeDetail::projectId.name to projectId,
                    NodeDetail::repoName.name to repoName,
                    RuleArtifact::name.name to resourceKey.substringAfterLast(CharPool.SLASH)
                )
                return RuleMatcher.match(rule, valuesToMatch)
            }

            if ((event.type == EventType.VERSION_CREATED || event.type == EventType.VERSION_UPDATED)) {
                val valuesToMatch = mapOf(
                    PackageSummary::projectId.name to projectId,
                    PackageSummary::repoName.name to repoName,
                    PackageSummary::type.name to data[VersionCreatedEvent::packageType.name] as String,
                    RuleArtifact::name.name to data[VersionCreatedEvent::packageName.name] as String,
                    RuleArtifact::version.name to data[VersionCreatedEvent::packageVersion.name] as String
                )
                return RuleMatcher.match(rule, valuesToMatch)
            }
        }

        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScanEventConsumer::class.java)
    }
}
