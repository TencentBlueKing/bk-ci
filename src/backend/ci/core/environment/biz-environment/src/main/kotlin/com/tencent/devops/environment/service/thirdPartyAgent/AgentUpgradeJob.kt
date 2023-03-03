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

package com.tencent.devops.environment.service.thirdPartyAgent

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.environment.agent.AgentUpgradeType
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.service.thirdPartyAgent.upgrade.AgentPropsScope
import com.tencent.devops.environment.service.thirdPartyAgent.upgrade.AgentScope
import com.tencent.devops.environment.service.thirdPartyAgent.upgrade.ProjectScope
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Suppress("UNUSED")
class AgentUpgradeJob @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val agentPropsScope: AgentPropsScope,
    private val agentScope: AgentScope,
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val client: Client,
    private val projectScope: ProjectScope
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AgentUpgradeJob::class.java)
        private const val LOCK_KEY = "env_cron_updateCanUpgradeAgentList"
        private const val MINUTES_10 = 600L
        private const val SECONDS_10 = 10000L
        private const val SECONDS_30 = 30000L
    }

    @Scheduled(initialDelay = SECONDS_10, fixedDelay = SECONDS_30)
    fun updateCanUpgradeAgentList() {
        val watcher = Watcher("updateCanUpgradeAgentList")
        logger.info("updateCanUpgradeAgentList start")
        watcher.start("try lock")
        val lock = RedisLock(redisOperation, lockKey = LOCK_KEY, expiredTimeInSeconds = MINUTES_10)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip")
                return
            }
            watcher.start("get maxParallelCount")
            val maxParallelCount = agentPropsScope.getMaxParallelUpgradeCount()
            if (maxParallelCount < 1) {
                logger.warn("parallel count set to zero")
                agentScope.setCanUpgradeAgents(listOf())
                return
            }

            watcher.start("listCanUpdateAgents")
            val canUpgradeAgents = listCanUpdateAgents(maxParallelCount) ?: return

            if (canUpgradeAgents.isNotEmpty()) {
                watcher.start("setCanUpgradeAgents")
                agentScope.setCanUpgradeAgents(canUpgradeAgents.map { it.id })
            }
        } catch (ignore: Throwable) {
            logger.warn("update can upgrade agent list failed", ignore)
        } finally {
            lock.unlock()
            logger.info("updateCanUpgradeAgentList| $watcher")
        }
    }

    private fun listCanUpdateAgents(maxParallelCount: Int): List<TEnvironmentThirdpartyAgentRecord>? {
        val currentVersion = agentPropsScope.getWorkerVersion().ifBlank {
            logger.warn("invalid server agent version")
            return null
        }

        val currentMasterVersion = agentPropsScope.getAgentVersion().ifBlank {
            logger.warn("invalid server agent version")
            return null
        }

        val currentDockerInitFileMd5 = agentPropsScope.getDockerInitFileMd5()

        val importOKAgents = thirdPartyAgentDao.listByStatus(
            dslContext = dslContext,
            status = setOf(AgentStatus.IMPORT_OK)
        ).toSet()
        val needUpgradeAgents = importOKAgents.filter {
            // #5806 #5045 解决worker过老，或者异常，导致拿不到版本号，而无法自愈或升级的问题
            // it.version.isNullOrBlank() || it.masterVersion.isNullOrBlank() -> false
            if (checkProjectRouter(it.projectId) && checkProjectUpgrade(it.projectId)) {
                checkCanUpgrade(
                    goAgentCurrentVersion = currentMasterVersion,
                    workCurrentVersion = currentVersion,
                    currentDockerInitFileMd5 = currentDockerInitFileMd5,
                    record = it
                )
            } else {
                false
            }
        }
        return if (needUpgradeAgents.size > maxParallelCount) {
            needUpgradeAgents.subList(0, maxParallelCount)
        } else {
            needUpgradeAgents
        }
    }

    private fun checkProjectRouter(projectId: String): Boolean {
        return client.get(ServiceProjectTagResource::class).checkProjectRouter(projectId).data ?: false
    }

    @Suppress("ComplexMethod")
    private fun checkCanUpgrade(
        goAgentCurrentVersion: String,
        workCurrentVersion: String,
        currentDockerInitFileMd5: String,
        record: TEnvironmentThirdpartyAgentRecord
    ): Boolean {
        AgentUpgradeType.values().forEach { type ->
            val res = when (type) {
                AgentUpgradeType.GO_AGENT -> {
                    goAgentCurrentVersion.trim() != record.masterVersion.trim()
                }

                AgentUpgradeType.WORKER -> {
                    workCurrentVersion.trim() != record.version.trim()
                }

                AgentUpgradeType.JDK -> {
                    val props = agentPropsScope.parseAgentProps(record.agentProps) ?: return@forEach
                    val currentJdkVersion =
                        agentPropsScope.getJdkVersion(record.os, props.arch)?.ifBlank { null } ?: return@forEach
                    if (props.jdkVersion.size > 2) {
                        currentJdkVersion.trim() != props.jdkVersion.last().trim()
                    } else {
                        false
                    }
                }

                AgentUpgradeType.DOCKER_INIT_FILE -> {
                    if (currentDockerInitFileMd5.isBlank()) {
                        return@forEach
                    }
                    val props = agentPropsScope.parseAgentProps(record.agentProps) ?: return@forEach
                    if (props.dockerInitFileInfo?.needUpgrade != true) {
                        return@forEach
                    }
                    (props.dockerInitFileInfo.fileMd5.isNotBlank() &&
                            props.dockerInitFileInfo.fileMd5.trim() != currentDockerInitFileMd5.trim())
                }
            }
            if (res) {
                return true
            }
        }
        return false
    }

    /**
     * 校验这个agent所属的项目是否可以进行升级或者其他属性
     * @return true 可以升级 false 不能进行升级
     */
    private fun checkProjectUpgrade(projectId: String): Boolean {
        // 校验不升级项目，这些项目不参与Agent升级
        if (projectScope.checkDenyUpgradeProject(projectId)) {
            return false
        }

        // 校验是否在优先升级的项目列表中，如果不在里面并且优先升级项目的列表为空也允许Agent升级。
        return projectScope.checkInPriorityUpgradeProjectOrEmpty(projectId)
    }
}
