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
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.job.AgentVersion
import com.tencent.devops.environment.pojo.job.DisplayNameInfo
import com.tencent.devops.environment.pojo.job.HostIdAndCloudAreaIdInfo
import com.tencent.devops.environment.pojo.job.ccres.CCInfo
import com.tencent.devops.environment.pojo.job.req.OpOperateReq
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service("StockDataUpdateService")
class StockDataUpdateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val queryFromCCService: QueryFromCCService,
    private val queryAgentStatusService: QueryAgentStatusService,
    private val opService: OpService,
    private val redisOperation: RedisOperation
) : IStockDataUpdateService {

    companion object {
        private val logger = LoggerFactory.getLogger(StockDataUpdateService::class.java)

        private const val DEFAULT_PAGE_SIZE = 100
        private const val AGENT_NOT_INSTALLED_TAG = false
        private const val AGENT_ABNORMAL_NODE_STATUS = 0
        private const val AGENT_NORMAL_NODE_STATUS = 1

        private const val EXPIRATION_TIME_OF_THE_LOCK = 200L
        private const val SCHEDULED_WRITE_DISPLAY_NAME_TIMEOUT_LOCK_KEY = "scheduled_write_display_name_timeout_lock"
        private const val SCHEDULED_UPDATE_AGENT_TIMEOUT_LOCK_KEY = "scheduled_update_agent_timeout_lock"
    }

    /**
     * checkDeployNodesIsInCC:
     * 后台定时轮询机器状态，看机器是否在CC中
     * 轮询T_NODE表中 NODE_TYPE为"部署"的记录。部署：CMDB("CMDB")，UNKNOWN("未知")，OTHER("其他")
     * 在不在cc -> cc的host_id和云区域id是否改变
     * cron：每天执行一次，上午10点执行。
     */
    override fun checkDeployNodes() {
        checkDeployNodesIsInCC()
    }

    /**
     * updateAgent:
     * 定时任务：agent状态/版本 轮询 + 差量更新
     * 条件：NODE_TYPE为cmdb的，查询该节点的agent安装状态以及版本，并对比差异更新。
     * 分组执行，每次遍历1000条记录。
     * cron：每小时执行一次。
     */
    @Scheduled(cron = "0 * * * * 1-5")
    fun scheduledUpdateAgent() {
        taskWithRedisLock(SCHEDULED_UPDATE_AGENT_TIMEOUT_LOCK_KEY, ::updateAgent)
    }

    /**
     * writeDisplayName:
     * display_name为空的：拼接节点类型、node hash值、nodeId这三个字段，写入display_name。
     * 分组执行，每次遍历100条记录。
     * 执行一次就行。提供apigw接口。
     */
    fun writeDisplayNameOnce() {
        taskWithRedisLock(SCHEDULED_WRITE_DISPLAY_NAME_TIMEOUT_LOCK_KEY, ::writeDisplayName)
    }

    private fun updateAgent() {
        val countCmdbNodes = nodeDao.countCmdbNodes(dslContext)
        if (logger.isDebugEnabled) logger.debug("[updateAgent]countCmdbNodes:$countCmdbNodes.")
        countCmdbNodes.takeIf { it > 0 }?.run {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countCmdbNodes.toLong())
            for (page in 1..totalPages) {
                val cmdbNodesRecords = nodeDao.getCmdbNodes(dslContext, page - 1, DEFAULT_PAGE_SIZE)
                val existAgentVersionList = cmdbNodesRecords.filter {
                    val opInfo = opService.operateOpProject(
                        "", OpOperateReq(2, listOf(it.projectId))
                    ).projGrayStatus?.get(0)
                    it.projectId == opInfo?.englishName && true == opInfo?.projGrayStatus
                }.map {
                    AgentVersion(
                        ip = it.nodeIp,
                        bkHostId = it.hostId,
                        installedTag = NodeStatus.NOT_INSTALLED.name != it.nodeStatus,
                        version = it.agentVersion,
                        status = if (it.agentStatus) 1 else 0
                    )
                }
                if (logger.isDebugEnabled) logger.debug("[updateAgent]existAgentVersionList:$existAgentVersionList.")
                val ipToExistAgentVersion = existAgentVersionList.associateBy { it.ip }
                val newAgentVersionList = queryAgentStatusService.getAgentVersions(existAgentVersionList)
                if (logger.isDebugEnabled) logger.debug("[updateAgent]newAgentVersionList:$newAgentVersionList.")
                // 判断 newAgentVersionList 和 existAgentVersionList 是否一致，不一致则更新对应数据库表
                val agentUpdateList = newAgentVersionList?.filterNot {
                    it.installedTag == ipToExistAgentVersion[it.ip]?.installedTag &&
                        it.version == ipToExistAgentVersion[it.ip]?.version &&
                        it.status == ipToExistAgentVersion[it.ip]?.status
                }
                if (logger.isDebugEnabled) logger.debug("[updateAgent]agentUpdateList:$agentUpdateList.")
                if (!agentUpdateList.isNullOrEmpty()) {
                    batchUpdateAgent(cmdbNodesRecords, agentUpdateList)
                }
            }
        }
    }

    private fun batchUpdateAgent(cmdbNodesRecords: List<TNodeRecord>, agentUpdateList: List<AgentVersion>) {
        val ipToAgentUpdateList = agentUpdateList.associateBy { it.ip }
        val agentUpdateIpList = agentUpdateList.mapNotNull { it.ip }
        val agentUpdateHostIdList = agentUpdateList.mapNotNull { it.bkHostId }
        val agentUpdateRecords = cmdbNodesRecords.filter {
            agentUpdateIpList.contains(it.nodeIp) || agentUpdateHostIdList.contains(it.hostId)
        }.map {
            it.nodeStatus =
                if (AGENT_NOT_INSTALLED_TAG == ipToAgentUpdateList[it.nodeIp]?.installedTag)
                    NodeStatus.NOT_INSTALLED.name
                else if (AGENT_ABNORMAL_NODE_STATUS == ipToAgentUpdateList[it.nodeIp]?.status)
                    NodeStatus.ABNORMAL.name
                else if (AGENT_NORMAL_NODE_STATUS == ipToAgentUpdateList[it.nodeIp]?.status)
                    NodeStatus.NORMAL.name
                else null
            it.agentStatus = AGENT_NORMAL_NODE_STATUS == ipToAgentUpdateList[it.nodeIp]?.status
            if (logger.isDebugEnabled)
                logger.debug(
                    "[batchUpdateAgent]ip:${it.nodeIp}, agentStatus:${ipToAgentUpdateList[it.nodeIp]?.status}," +
                        "${AGENT_NORMAL_NODE_STATUS == ipToAgentUpdateList[it.nodeIp]?.status}"
                )
            it.agentVersion = ipToAgentUpdateList[it.nodeIp]?.version
            it.lastModifyTime = LocalDateTime.now()
            it
        }
        if (logger.isDebugEnabled) logger.debug("[batchUpdateAgent]agentUpdateRecords:$agentUpdateRecords.")
        nodeDao.batchUpdateNodeRecords(dslContext, agentUpdateRecords)
    }

    private fun writeDisplayName() {
        val countDisplayNameEmptyNodes = nodeDao.countDisplayNameEmptyNodes(dslContext)
        if (logger.isDebugEnabled)
            logger.debug("[writeDisplayName]countDisplayNameEmptyNodes:$countDisplayNameEmptyNodes.")
        if (0 < countDisplayNameEmptyNodes) {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countDisplayNameEmptyNodes.toLong())
            for (page in 1..totalPages) {
                val displayNameNullNodeRecords =
                    nodeDao.getNodesWhoseDisplayNameIsEmpty(dslContext, page - 1, DEFAULT_PAGE_SIZE)
                val nodeDisplayNameInfoList = displayNameNullNodeRecords.map {
                    DisplayNameInfo(
                        nodeId = it.value1(),
                        nodeType = it.value2(),
                        nodeHashId = it.value3(),
                        displayName = it.value2() + "-" + it.value3() + "-" + it.value1().toString()
                    )
                }
                val nodeIdList = nodeDisplayNameInfoList.mapNotNull { it.nodeId }
                val nodeIdToRecordMap = nodeDisplayNameInfoList.associateBy { it.nodeId }
                val nodeRecords = nodeDao.getNodesByNodeId(dslContext, nodeIdList)
                nodeRecords.forEach {
                    it.displayName = nodeIdToRecordMap[it.nodeId]?.displayName
                    it.lastModifyTime = LocalDateTime.now()
                }
                nodeDao.batchUpdateNodeRecords(dslContext, nodeRecords)
            }
        } else {
            if (logger.isDebugEnabled) logger.debug("[writeDisplayName] There is no node with empty DisplayName.")
        }
    }

    fun checkDeployNodesIsInCC() {
        val countHostIdNotNullRecord = nodeDao.countDeployNodesInCmdb(dslContext)
        if (logger.isDebugEnabled)
            logger.debug("[checkDeployNodesIsInCC]countHostIdNotNullRecord:$countHostIdNotNullRecord.")
        countHostIdNotNullRecord.takeIf { it > 0 }.run {
            val totalPagesHostIdNotNull = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countHostIdNotNullRecord.toLong())
            for (pageHostIdNotNull in 1..totalPagesHostIdNotNull) {
                checkDeployNodesIsInCCByPage(pageHostIdNotNull)
            }
        }
    }

    fun checkDeployNodesIsInCCByPage(page: Int) {
        // 1. 节点record："部署"类型
        val nodeRecords = nodeDao.getDeployNodesInCmdbLimit(dslContext, page, DEFAULT_PAGE_SIZE)
        // 要判断在不在cc中的 所有节点ip
        val nodeIpList = nodeRecords.map { it.value3() }.toSet()
        // cc记录
        val nodeCCInfoList = nodeIpList.takeIf { it.isNotEmpty() }.run {
            queryFromCCService.queryCCListHostWithoutBizByInRules(
                listOf(QueryFromCCService.FIELD_BK_HOST_ID, QueryFromCCService.FIELD_BK_HOST_INNERIP), nodeIpList, QueryFromCCService.FIELD_BK_HOST_INNERIP
            ).data?.info
        }
        var ipToCCInfoMap: Map<String?, CCInfo>?
        nodeCCInfoList.takeIf { !it.isNullOrEmpty() }.run {
            // ip - cc记录 映射
            ipToCCInfoMap = nodeCCInfoList!!.associateBy { it.bkHostInnerip }
            // 2.1 在CC - 改为NORMAL
            val inCCIpList = nodeCCInfoList.mapNotNull { it.bkHostInnerip }
            inCCIpList.takeIf { it.isNotEmpty() }.run {
                nodeDao.updateNodeInCCByIp(dslContext, inCCIpList)
                // 4. CC中信息（host_id和云区域id）改变 - 更新信息，不变 - 不操作
                val nodeUpdateInfoList = nodeRecords.filterNot {
                    it.value4() == ipToCCInfoMap!![it.value3()]?.bkHostId
                        && it.value5() == ipToCCInfoMap!![it.value3()]?.bkCloudId?.toLong()
                }.takeIf { it.isNotEmpty() }!!.map {
                    HostIdAndCloudAreaIdInfo(
                        nodeId = it.value1(),
                        bkCloudId = ipToCCInfoMap!![it.value3()]?.bkCloudId?.toLong(),
                        bkHostId = ipToCCInfoMap!![it.value3()]?.bkHostId
                    )
                }
                nodeDao.updateHostIdAndCloudAreaIdByNodeId(dslContext, nodeUpdateInfoList)
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
                if (logger.isDebugEnabled) logger.debug("[taskWithRedisLock]Locked.")
                operation()
            } else {
                if (logger.isDebugEnabled) logger.debug("[taskWithRedisLock]Lock failed.")
            }
        } catch (e: Throwable) {
            logger.error("[taskWithRedisLock]exception: ", e)
        } finally {
            redisLock.unlock()
        }
    }
}