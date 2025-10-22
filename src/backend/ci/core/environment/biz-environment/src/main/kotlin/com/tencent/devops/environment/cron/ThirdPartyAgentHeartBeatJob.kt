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

package com.tencent.devops.environment.cron

import com.tencent.devops.common.api.enums.AgentAction
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.util.LoopUtil
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.environment.constant.THIRD_PARTY_AGENT_HEARTBEAT_INTERVAL
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.service.NodeWebsocketService
import com.tencent.devops.environment.service.thirdpartyagent.ThirdPartAgentService
import com.tencent.devops.environment.utils.ThirdPartyAgentHeartbeatUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.math.ceil
import kotlin.math.max

@Component
@Suppress("ALL", "UNUSED")
class ThirdPartyAgentHeartBeatJob @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val nodeDao: NodeDao,
    private val thirdPartyAgentHeartbeatUtils: ThirdPartyAgentHeartbeatUtils,
    private val redisOperation: RedisOperation,
    private val webSocketDispatcher: WebSocketDispatcher,
    private val nodeWebsocketService: NodeWebsocketService,
    private val thirdPartAgentService: ThirdPartAgentService
) {

    @Scheduled(initialDelay = 5000, fixedDelay = 10000)
    fun heartbeat() {
        RedisLock(redisOperation = redisOperation, lockKey = LOCK_KEY, expiredTimeInSeconds = 600).use { lock ->

            if (!lock.tryLock()) {
                logger.info("$LOCK_KEY had locked!")
                return
            }
            checkOKAgent()

            checkExceptionAgent()

            checkUnImportAgent()
        }
    }

    private fun checkOKAgent() {
        val totalRecordCount = thirdPartyAgentDao.countAgentByStatus(dslContext, setOf(AgentStatus.IMPORT_OK))
        val lc = max(ceil(totalRecordCount / PageUtil.DEFAULT_PAGE_SIZE.toFloat()).toInt(), 1) // 算出循环次数
        val vo = LoopUtil.LoopVo<Long, Any>(id = 0L, data = true, thresholdMills = MAX_LOOP_MILLS, thresholdCount = lc)
        justDoItByLoop(vo = vo, tip = "checkOnlineAgent", func = this::checkOnlineAgent)
    }

    private fun checkOnlineAgent(vo: LoopUtil.LoopVo<Long, Any>) {
        val records = thirdPartyAgentDao.listByStatusGtId(dslContext, setOf(AgentStatus.IMPORT_OK), vo.id)
        records.ifEmpty {
            vo.finish = true
            return
        }.forEach { record ->
            if (record.nodeId == null) {
                vo.id = max(vo.id, record.id)
                return@forEach
            }
            val heartbeatTime = thirdPartyAgentHeartbeatUtils.getHeartbeatTime(record.id, record.projectId)
            // 50s
            if (heartbeatTime != null && System.currentTimeMillis() - heartbeatTime > OK_AGENT_INTERVAL_MILLS) {
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    val nodeRecord = nodeDao.get(context, record.projectId, record.nodeId)
                    if (nodeRecord == null || nodeRecord.nodeStatus == NodeStatus.DELETED.name) {
                        deleteAgent(context, record.projectId, record.id)
                        return@transaction
                    }
                    thirdPartyAgentDao.updateStatus(
                        dslContext = context,
                        id = record.id,
                        nodeId = null,
                        projectId = record.projectId,
                        status = AgentStatus.IMPORT_EXCEPTION
                    )
                    thirdPartAgentService.addAgentAction(
                        projectId = record.projectId,
                        agentId = record.id,
                        action = AgentAction.OFFLINE
                    )
                    nodeDao.updateNodeStatus(context, setOf(record.nodeId), NodeStatus.ABNORMAL)
                }
            }

            vo.id = max(vo.id, record.id)
        }
    }

    private fun checkUnImportAgent() {
        val totalRecordCount = thirdPartyAgentDao.countAgentByStatus(dslContext, setOf(AgentStatus.UN_IMPORT_OK))
        val lc = max(ceil(totalRecordCount / PageUtil.DEFAULT_PAGE_SIZE.toFloat()).toInt(), 1) // 算出循环次数
        val vo = LoopUtil.LoopVo<Long, Any>(id = 0L, data = true, thresholdMills = MAX_LOOP_MILLS, thresholdCount = lc)
        justDoItByLoop(vo = vo, tip = "checkUnImportAgent", func = this::checkUnImportAgent)
    }

    private fun checkUnImportAgent(vo: LoopUtil.LoopVo<Long, Any>) {
        val records = thirdPartyAgentDao.listByStatusGtId(dslContext, setOf(AgentStatus.UN_IMPORT_OK), vo.id)
        records.ifEmpty {
            vo.finish = true // 没有数据完成任务
            return
        }.forEach { record ->
            vo.id = max(vo.id, record.id)
            val heartbeatTime = thirdPartyAgentHeartbeatUtils.getHeartbeatTime(record.id, record.projectId)
                ?: return@forEach
            val escape = System.currentTimeMillis() - heartbeatTime
            if (escape > UNIMPORT_AGENT_INTERVAL_MILLS) {
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    thirdPartyAgentDao.updateStatus(
                        dslContext = context,
                        id = record.id,
                        nodeId = null,
                        projectId = record.projectId,
                        status = AgentStatus.UN_IMPORT,
                        expectStatus = AgentStatus.UN_IMPORT_OK
                    )
                }
            }
        }
    }

    private fun checkExceptionAgent() {
        val totalRecordCount = thirdPartyAgentDao.countAgentByStatus(dslContext, setOf(AgentStatus.IMPORT_EXCEPTION))
        val lc = max(ceil(totalRecordCount / PageUtil.DEFAULT_PAGE_SIZE.toFloat()).toInt(), 1) // 算出循环次数
        val vo = LoopUtil.LoopVo<Long, Any>(id = 0L, data = true, thresholdMills = MAX_LOOP_MILLS, thresholdCount = lc)
        justDoItByLoop(vo, "checkExceptionAgent", this::deleteExceptionAgent)
    }

    private fun deleteExceptionAgent(vo: LoopUtil.LoopVo<Long, Any>) {
        val records = thirdPartyAgentDao.listByStatusGtId(dslContext, setOf(AgentStatus.IMPORT_EXCEPTION), vo.id)
        records.ifEmpty {
            vo.finish = true // 没有数据完成任务
            return
        }.forEach { record ->
            vo.id = max(vo.id, record.id)
            if (record.nodeId == null) {
                return@forEach
            }
            val nodeRecord = nodeDao.get(dslContext, record.projectId, record.nodeId)
            if (nodeRecord == null || nodeRecord.nodeStatus == NodeStatus.DELETED.name) {
                deleteAgent(dslContext, record.projectId, record.id)
            }
        }
    }

    private fun deleteAgent(dslContext: DSLContext, projectId: String, agentId: Long) {
        logger.info("Trying to delete the agent($agentId) of project($projectId)")
        thirdPartyAgentDao.delete(dslContext, agentId, projectId)
    }

    private fun <T> justDoItByLoop(vo: LoopUtil.LoopVo<Long, T>, tip: String, func: (LoopUtil.LoopVo<Long, T>) -> Any) {
        val watcher = Watcher("${tip}_${DateTimeUtil.toDateTime(LocalDateTime.now())}")
        watcher.use {
            watcher.start()
            do {
                val met = LoopUtil.doLoop(vo) { func(it) }
                logger.info("$tip| metrics: $met")
            } while (!vo.finish)
        }
        LogUtils.printCostTimeWE(watcher, OK_AGENT_INTERVAL_MILLS, MAX_LOOP_MILLS)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentHeartBeatJob::class.java)
        private const val LOCK_KEY = "env_cron_agent_heartbeat_check"
        private const val OK_AGENT_INTERVAL_MILLS = 10 * THIRD_PARTY_AGENT_HEARTBEAT_INTERVAL * 1000 // 10个心跳周期内未上报的
        private const val UNIMPORT_AGENT_INTERVAL_MILLS = 2 * THIRD_PARTY_AGENT_HEARTBEAT_INTERVAL * 1000 // 2个心跳周期内未完成导入
        private const val MAX_LOOP_MILLS = 10 * 60 * 1000L // 10 minutes
    }
}
