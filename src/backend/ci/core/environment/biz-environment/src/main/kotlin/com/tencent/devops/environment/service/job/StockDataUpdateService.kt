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
import com.tencent.devops.environment.constant.T_NODE_CLOUD_AREA_ID
import com.tencent.devops.environment.constant.T_NODE_HOST_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_HASH_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.constant.T_NODE_NODE_TYPE
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.job.AgentVersion
import com.tencent.devops.environment.pojo.job.AgentVersionInfo
import com.tencent.devops.environment.pojo.job.DisplayNameInfo
import com.tencent.devops.environment.pojo.job.CCUpdateInfo
import com.tencent.devops.environment.pojo.job.UpdateTNodeInfo
import com.tencent.devops.environment.pojo.job.ccres.CCInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service("StockDataUpdateService")
class StockDataUpdateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val queryFromCCService: QueryFromCCService,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val redisOperation: RedisOperation,
    private val queryAgentStatusService: QueryAgentStatusService
) : IStockDataUpdateService {

    companion object {
        private val logger = LoggerFactory.getLogger(StockDataUpdateService::class.java)
        private const val WRITE_DISPLAY_NAME_TIMEOUT_LOCK_KEY = "write_display_name_timeout_lock"
        private const val UPDATE_DEVOPS_AGENT_TIMEOUT_LOCK_KEY = "update_devops_agent_timeout_lock"

        private const val DEFAULT_PAGE_SIZE = 100
        private const val EXPIRATION_TIME_OF_THE_LOCK = 200L

        const val AGENT_ABNORMAL_NODE_STATUS = 0
        const val AGENT_NORMAL_NODE_STATUS = 1
        const val AGENT_NOT_INSTALLED_TAG = false

        const val NS_TO_S = 1000000000
    }

    /**
     * checkDeployNodesIsInCC:
     * 后台定时轮询机器状态，看机器是否在CC中
     * 轮询T_NODE表中 NODE_TYPE为"部署"的记录。部署：CMDB("CMDB")，UNKNOWN("未知")，OTHER("其他")
     * 在不在cc -> cc的host_id和云区域id是否改变
     * cron：每小时执行一次。
     */
    override fun checkDeployNodes() {
        checkDeployNodesIsInCC()
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

    fun checkDeployNodesIsInCC() {
        val countHostIdNotNullRecord = nodeDao.countDeployNodesInCmdb(dslContext)
        logger.info("Check deploy node(s) is in CC, node(s) count:$countHostIdNotNullRecord.")
        countHostIdNotNullRecord.takeIf { it > 0 }.run {
            val totalPagesHostIdNotNull = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countHostIdNotNullRecord.toLong())
            val startTime = LocalDateTime.now()
            for (pageHostIdNotNull in 1..totalPagesHostIdNotNull) {
                checkDeployNodesIsInCCByPage(pageHostIdNotNull)
            }
            logger.info(
                "[checkDeployNodesIsInCC]total time: " +
                    "${Duration.between(startTime, LocalDateTime.now()).toNanos().toDouble() / NS_TO_S}s"
            )
        }
    }

    fun checkDeployNodesIsInCCByPage(page: Int) {
        // 1. 节点record："部署"类型
        val nodeRecords = nodeDao.getDeployNodesInCmdbLimit(dslContext, page, DEFAULT_PAGE_SIZE)
        // 要判断在不在cc中的 所有节点ip
        val nodeIpList = nodeRecords.map { it[T_NODE_NODE_IP] as String }.toSet()
        // cc记录
        val nodeCCInfoList = nodeIpList.takeIf { it.isNotEmpty() }.run {
            queryFromCCService.queryCCListHostWithoutBizByInRules(
                listOf(QueryFromCCService.FIELD_BK_HOST_ID, QueryFromCCService.FIELD_BK_HOST_INNERIP),
                nodeIpList,
                QueryFromCCService.FIELD_BK_HOST_INNERIP
            ).data?.info
        }
        var ipToCCInfoMap: Map<String?, CCInfo>?
        nodeCCInfoList.takeIf { !it.isNullOrEmpty() }.run {
            // ip - cc记录 映射
            ipToCCInfoMap = nodeCCInfoList!!.associateBy { it.bkHostInnerip }
            // 2.1 在CC（且状态此时为NOT_IN_CC/NOT_IN_CMDB） - 查询节点agent状态并更新
            val inCCIpList = nodeCCInfoList.mapNotNull { it.bkHostInnerip }
            inCCIpList.takeIf { it.isNotEmpty() }.run {
                val ipToAgentVersionInfoMap = queryAgentStatusService.getAgentVersions(
                    nodeCCInfoList.map {
                        AgentVersion(ip = it.bkHostInnerip, bkHostId = it.bkHostId)
                    }
                )?.associateBy { it.ip }
                val ipToNodeStatus = mutableMapOf<String, String>()
                inCCIpList.map {
                    ipToNodeStatus[it] =
                        if (AGENT_NOT_INSTALLED_TAG == ipToAgentVersionInfoMap?.get(it)?.installedTag)
                            NodeStatus.NOT_INSTALLED.name
                        else if (AGENT_ABNORMAL_NODE_STATUS == ipToAgentVersionInfoMap?.get(it)?.status)
                            NodeStatus.ABNORMAL.name
                        else if (AGENT_NORMAL_NODE_STATUS == ipToAgentVersionInfoMap?.get(it)?.status)
                            NodeStatus.NORMAL.name
                        else
                            NodeStatus.NOT_INSTALLED.name
                }
                nodeDao.updateNodeInCCByIp(dslContext, ipToNodeStatus)
                // 4. CC中信息（host_id和云区域id）改变 - 更新信息，不变 - 不操作
                val nodeUpdateInfoList = nodeRecords.filterNot {
                    it[T_NODE_HOST_ID] as? Long == ipToCCInfoMap!![it[T_NODE_NODE_IP] as String]?.bkHostId &&
                        it[T_NODE_CLOUD_AREA_ID] as? Long == ipToCCInfoMap!![it[T_NODE_NODE_IP] as String]
                        ?.bkCloudId?.toLong()
                }.takeIf { it.isNotEmpty() }?.map {
                    CCUpdateInfo(
                        nodeId = it[T_NODE_NODE_ID] as Long,
                        bkCloudId = ipToCCInfoMap!![it[T_NODE_NODE_IP] as String]?.bkCloudId?.toLong(),
                        bkHostId = ipToCCInfoMap!![it[T_NODE_NODE_IP] as String]?.bkHostId
                    )
                }
                if (!nodeUpdateInfoList.isNullOrEmpty()){
                    nodeDao.updateHostIdAndCloudAreaIdByNodeId(dslContext, nodeUpdateInfoList)
                }
            }
        }
        // 2.2 不在cc中: 置空 host_id 和 云区域id，且 NODE_STATUS 改成 NOT_IN_CC
        val invalidIpList = nodeIpList.filterNot { ipToCCInfoMap?.containsKey(it) ?: false }
        invalidIpList.takeIf { it.isNotEmpty() }.run {
            nodeDao.updateNodeNotInCCByIp(dslContext, invalidIpList)
        }
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
}