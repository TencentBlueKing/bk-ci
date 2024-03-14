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

package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.constant.T_ENVIRONMENT_THIRDPARTY_AGENT_MASTER_VERSION
import com.tencent.devops.environment.constant.T_ENVIRONMENT_THIRDPARTY_AGENT_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_HASH_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_TYPE
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.pojo.job.AgentVersionInfo
import com.tencent.devops.environment.pojo.job.DisplayNameInfo
import com.tencent.devops.environment.pojo.job.UpdateTNodeInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service("StockDataUpdateService")
class StockDataUpdateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StockDataUpdateService::class.java)

        private const val WRITE_DISPLAY_NAME_TIMEOUT_LOCK_KEY = "write_display_name_timeout_lock"
        private const val UPDATE_DEVOPS_AGENT_TIMEOUT_LOCK_KEY = "update_devops_agent_timeout_lock"

        private const val DEFAULT_PAGE_SIZE = 100
        private const val EXPIRATION_TIME_OF_THE_LOCK = 200L
    }

    /**
     * writeDisplayName:
     * display_name为空的：拼接节点类型、node hash值、nodeId这三个字段，写入display_name。
     * 分组执行，每次遍历100条记录。
     * 存量数据更新任务：执行一次。提供apigw接口。
     */
    fun writeDisplayNameOnce() {
        taskWithRedisLock(WRITE_DISPLAY_NAME_TIMEOUT_LOCK_KEY, ::writeDisplayName)
    }

    /**
     * updateAgent:
     * 定时任务：构建机 - 查询蓝盾agent版本 + 差量更新
     * 条件：NODE_TYPE为"构建"的，在T_ENVIRONMENT_THIRDPARTY_AGENT表中查询该节点的蓝盾agent版本，并对比T_NODE表中的状态差异更新。
     * 分组执行，每次遍历100条记录。
     * cron：执行一次。提供apigw接口。
     */
    fun updateDevopsAgentOnce() {
        taskWithRedisLock(UPDATE_DEVOPS_AGENT_TIMEOUT_LOCK_KEY, ::updateDevopsAgent)
    }

    fun taskWithRedisLock(lockKey: String, operation: () -> Unit) {
        val redisLock = RedisLock(redisOperation, lockKey, EXPIRATION_TIME_OF_THE_LOCK)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("[taskWithRedisLock]Locked.")
                operation()
            } else {
                logger.info("[taskWithRedisLock]Lock failed.")
            }
        } catch (e: Throwable) {
            logger.error("[taskWithRedisLock]exception: ", e)
        } finally {
            redisLock.unlock()
        }
    }

    private fun updateDevopsAgent() {
        val countBuildNodes = nodeDao.countBuildNodes(dslContext)
        logger.info("Update devops agent, node(s) count: $countBuildNodes.")
        countBuildNodes.takeIf { it > 0 }.run {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countBuildNodes.toLong())
            for (page in 1..totalPages) {
                val buildNodeRecords = nodeDao.getBuildNodesLimit(dslContext, page, DEFAULT_PAGE_SIZE)
                val buildNodeIdList = buildNodeRecords.mapNotNull { it[T_NODE_NODE_ID] as? Long }
                val buildNodesAgentVersionRecords = thirdPartyAgentDao.getAgentByNodeIdAllProj(
                    dslContext = dslContext,
                    nodeIdList = buildNodeIdList
                )
                val buildNodeUpdateInfo = buildNodesAgentVersionRecords.map {
                    AgentVersionInfo(
                        nodeId = it[T_ENVIRONMENT_THIRDPARTY_AGENT_NODE_ID] as Long,
                        agentVersion = it[T_ENVIRONMENT_THIRDPARTY_AGENT_MASTER_VERSION] as? String
                    )
                }
                nodeDao.updateBuildAgentVersionByNodeId(dslContext, buildNodeUpdateInfo)
            }
        }
    }

    private fun writeDisplayName() {
        val countDisplayNameEmptyNodes = nodeDao.countDisplayNameEmptyNodes(dslContext)
        logger.info("Write display name, node(s) count: $countDisplayNameEmptyNodes.")
        if (0 < countDisplayNameEmptyNodes) {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countDisplayNameEmptyNodes.toLong())
            for (page in 1..totalPages) {
                val displayNameNullNodeRecords =
                    nodeDao.getNodesWhoseDisplayNameIsEmpty(dslContext, page, DEFAULT_PAGE_SIZE)
                val nodeDisplayNameInfoList = displayNameNullNodeRecords.map {
                    DisplayNameInfo(
                        nodeId = it[T_NODE_NODE_ID] as Long,
                        nodeType = it[T_NODE_NODE_TYPE] as String,
                        nodeHashId = it[T_NODE_NODE_HASH_ID] as String,
                        displayName = it[T_NODE_NODE_TYPE] as String + "-" +
                            it[T_NODE_NODE_HASH_ID] as String + "-" + it[T_NODE_NODE_ID].toString()
                    )
                }
                val nodeIdList = nodeDisplayNameInfoList.mapNotNull { it.nodeId }
                val nodeIdToRecordMap = nodeDisplayNameInfoList.associateBy { it.nodeId }
                val nodeRecords = nodeDao.getNodesByNodeId(dslContext, nodeIdList)
                val updateTNodeInfo = nodeRecords.map {
                    val nodeId = it[T_NODE_NODE_ID] as Long
                    UpdateTNodeInfo(
                        nodeId = nodeId,
                        displayName = nodeIdToRecordMap[nodeId]?.displayName,
                        lastModifyTime = LocalDateTime.now()
                    )
                }
                nodeDao.batchUpdateDisplayName(dslContext, updateTNodeInfo)
            }
        } else {
            logger.info("There is no node with empty DisplayName.")
        }
    }
}