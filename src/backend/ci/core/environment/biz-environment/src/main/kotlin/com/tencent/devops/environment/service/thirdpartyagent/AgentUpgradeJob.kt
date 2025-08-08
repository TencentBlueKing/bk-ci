/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.util.LoopUtil
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.pojo.AgentUpgradeType
import com.tencent.devops.environment.service.thirdpartyagent.upgrade.AgentPropsScope
import com.tencent.devops.environment.service.thirdpartyagent.upgrade.AgentScope
import com.tencent.devops.environment.service.thirdpartyagent.upgrade.ProjectScope
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@Component
@Suppress("UNUSED", "LongParameterList", "ReturnCount")
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
        private const val MAX_UPGRADE_AGENT_COUNT = 500 // 单次最大升级数量500，防止错误的配置导致超出系统承载能力
        private val okStatus = setOf(AgentStatus.IMPORT_OK)
    }

    @Scheduled(initialDelay = SECONDS_10, fixedDelay = SECONDS_30)
    fun updateCanUpgradeAgentList() {
        val watcher = Watcher("updateCanUpgradeAgentList")
        logger.debug("updateCanUpgradeAgentList start")
        watcher.start("try lock")
        val lock = RedisLock(redisOperation, lockKey = LOCK_KEY, expiredTimeInSeconds = MINUTES_10)
        try {
            if (!lock.tryLock()) {
                logger.debug("get lock failed, skip")
                return
            }
            watcher.start("get maxParallelCount")
            var maxParallelCount = agentPropsScope.getMaxParallelUpgradeCount()
            if (maxParallelCount < 1) {
                logger.debug("parallel count set to zero")
                agentScope.setCanUpgradeAgents(listOf())
                return
            }
            // 硬保护防止配置错误导致数量过大，引起系统过载
            maxParallelCount = min(maxParallelCount, MAX_UPGRADE_AGENT_COUNT)

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

    @Suppress("NestedBlockDepth", "LongMethod")
    private fun listCanUpdateAgents(maxParallelCount: Int): Collection<TEnvironmentThirdpartyAgentRecord>? {
        val currentVersion = agentPropsScope.getWorkerVersion().ifBlank {
            logger.warn("invalid server worker version")
            return null
        }

        val currentMasterVersion = agentPropsScope.getAgentVersion().ifBlank {
            logger.warn("invalid server agent version")
            return null
        }

        val currentDockerInitFileMd5 = agentPropsScope.getDockerInitFileMd5()

        val vo = LoopUtil.LoopVo<Long, MutableSet<TEnvironmentThirdpartyAgentRecord>>(id = 0L, data = HashSet())

        // 对于优先升级的项目的 agent 也一并计入并且放到前面
        fetchPriorityUpgradeAgents(
            currentVersion = currentVersion,
            currentMasterVersion = currentMasterVersion,
            currentDockerInitFileMd5 = currentDockerInitFileMd5,
            maxParallelCount = maxParallelCount,
            vo = vo
        )
        //  经过前面fetchPriorityUpgradeAgents计算后，vo.data.size 不会超过 maxParallelCount，所以不可能小于0
        val remainingCount = maxParallelCount - vo.data.size
        // 为了严谨性防止出现负数，仍然把小于判断加入if，杜绝可能出现的情况
        if (remainingCount <= 0) {
            return vo.data
        }

        val limit = min(remainingCount, PageUtil.MAX_PAGE_SIZE) // 取最小的为单次分页查询数量

        vo.id = 0 // 从头开始遍历，避免因为前面取优先升级的agent，而忽略掉的之前可升级的agent
        vo.thresholdCount = max(ceil(remainingCount / limit.toFloat()).toInt(), vo.thresholdCount) // 循环次数以大为主，防止循环提前退出
        val metrics = LoopUtil.LoopMetrics(System.currentTimeMillis())
        do {
            val m = LoopUtil.doLoop(vo) {
                /*
                    2、消除原全量加载在线构建机的逻辑，改为按id增量查询，匹配至单次可升级最大数量即退出循环，
                    减少全量加载构建机记录带来的内存压力
                 */
                val recs = thirdPartyAgentDao.listByStatusGtId(
                    dslContext = dslContext, startId = vo.id,
                    status = okStatus, limit = limit
                )
                recs.ifEmpty {
                    vo.finish = true
                    return@doLoop
                }.forEach { record ->
                    vo.id = max(vo.id, record.id)
                    if (checkProjectRouter(record.projectId)) {
                        if (checkCanUpgrade(
                                goAgentCurrentVersion = currentMasterVersion,
                                workCurrentVersion = currentVersion,
                                currentDockerInitFileMd5 = currentDockerInitFileMd5,
                                record = record
                            )
                        ) {
                            vo.data.add(record)
                        }
                    }
                    if (vo.data.size >= remainingCount) {
                        vo.finish = true
                        return@doLoop
                    }
                }
            }
            metrics.add(m)
        } while (!vo.finish)

        logger.info("listCanUpdateAgents|metrics: $metrics, tc=${vo.thresholdCount}, agent_size: ${vo.data.size}")
        return vo.data
    }

    @Suppress("NestedBlockDepth", "LongMethod")
    private fun fetchPriorityUpgradeAgents(
        currentVersion: String,
        currentMasterVersion: String,
        currentDockerInitFileMd5: String,
        maxParallelCount: Int,
        vo: LoopUtil.LoopVo<Long, MutableSet<TEnvironmentThirdpartyAgentRecord>>,
    ) {
        val priorityUpgradeProjects = mutableSetOf<String>()
        priorityUpgradeProjects.addAll(projectScope.fetchInPriorityUpgradeProject(AgentUpgradeType.GO_AGENT))
        priorityUpgradeProjects.addAll(projectScope.fetchInPriorityUpgradeProject(AgentUpgradeType.WORKER))
        if (priorityUpgradeProjects.isNotEmpty()) {
            return
        }
        val limit = min(maxParallelCount, PageUtil.MAX_PAGE_SIZE)
        val metrics = LoopUtil.doLoop(vo) {
            val upImportOKAgents = thirdPartyAgentDao.listByStatusAndProjectGtId(
                dslContext = dslContext,
                projects = priorityUpgradeProjects,
                status = okStatus,
                startId = vo.id,
                limit = limit
            )
            upImportOKAgents.ifEmpty {
                vo.finish = true
                return@doLoop
            }.forEach { agentRecord ->
                vo.id = max(vo.id, agentRecord.id)
                if (checkProjectRouter(agentRecord.projectId)
                    && checkCanUpgrade(
                        goAgentCurrentVersion = currentMasterVersion,
                        workCurrentVersion = currentVersion,
                        currentDockerInitFileMd5 = currentDockerInitFileMd5,
                        record = agentRecord
                    )
                ) {
                    vo.data.add(agentRecord)
                }
                // 查询完所有数据 立即退出
                if (vo.data.size == maxParallelCount) {
                    vo.finish = true
                    return@doLoop
                }
            }
        }
        logger.info("fetchPriorityUpgradeAgents|metrics: $metrics, agent_size: ${vo.data.size}")
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
            // 校验这个项目下的这个类型是否可以升级
            if (!checkProjectUpgrade(record.projectId, type)) {
                return@forEach
            }
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
                    if (props.jdkVersion.isEmpty()) {
                        true
                    } else if (props.jdkVersion.size > 2) {
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
     * 校验这个agent所属的项目是否可以进行升级或者其他属性，只支持worker和agent设置，除worker外的类型都和agent设置走
     * @return true 可以升级 false 不能进行升级
     */
    private fun checkProjectUpgrade(projectId: String, type: AgentUpgradeType): Boolean {
        // 校验不升级项目，这些项目不参与Agent升级
        if (projectScope.checkDenyUpgradeProject(projectId, type)) {
            return false
        }
        // 校验是否在优先升级的项目列表中，如果不在里面并且优先升级项目的列表为空也允许Agent升级。
        return projectScope.checkInPriorityUpgradeProjectOrEmpty(projectId, type)
    }
}
