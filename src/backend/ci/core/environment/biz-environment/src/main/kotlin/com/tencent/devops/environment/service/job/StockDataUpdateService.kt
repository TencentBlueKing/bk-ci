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
import com.tencent.devops.environment.constant.T_NODE_AGENT_STATUS
import com.tencent.devops.environment.constant.T_NODE_AGENT_VERSION
import com.tencent.devops.environment.constant.T_NODE_CLOUD_AREA_ID
import com.tencent.devops.environment.constant.T_NODE_HOST_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_HASH_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.constant.T_NODE_NODE_STATUS
import com.tencent.devops.environment.constant.T_NODE_NODE_TYPE
import com.tencent.devops.environment.constant.T_NODE_PROJECT_ID
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.job.AgentVersion
import com.tencent.devops.environment.pojo.job.DisplayNameInfo
import com.tencent.devops.environment.pojo.job.HostIdAndCloudAreaIdInfo
import com.tencent.devops.environment.pojo.job.UpdateAgentInfo
import com.tencent.devops.environment.pojo.job.ccres.CCInfo
import com.tencent.devops.environment.pojo.job.req.OpOperateReq
import org.jooq.DSLContext
import org.jooq.Record7
import org.jooq.Result
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
    @Scheduled(cron = "0 * * * * ?")
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
                val existNodeIdToAgentVersionMap = cmdbNodesRecords.filter {
                    val opInfo = opService.operateOpProject(
                        "", OpOperateReq(2, listOf(it[T_NODE_PROJECT_ID] as String))
                    ).projGrayStatus?.get(0)
                    it[T_NODE_PROJECT_ID] as String == opInfo?.englishName && true == opInfo.projGrayStatus
                }.associate {
                    it[T_NODE_NODE_ID] as Long to
                        AgentVersion(
                            ip = it[T_NODE_NODE_IP] as String,
                            bkHostId = it[T_NODE_HOST_ID] as Long,
                            installedTag = NodeStatus.NOT_INSTALLED.name != it[T_NODE_NODE_STATUS] as String,
                            version = it[T_NODE_AGENT_VERSION] as String,
                            status = if (it[T_NODE_AGENT_STATUS] as Boolean) 1 else 0
                        )
                }
                val existAgentVersionList = existNodeIdToAgentVersionMap.values.toList()
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
                agentUpdateList.takeIf { !it.isNullOrEmpty() }.run {
                    batchUpdateAgent(existNodeIdToAgentVersionMap, agentUpdateList!!)
                }
            }
        }
    }

    private fun batchUpdateAgent(
        existNodeIdToAgentVersionMap: Map<Long, AgentVersion>,
        agentUpdateList: List<AgentVersion>
    ) {
        val ipToAgentUpdateList = agentUpdateList.associateBy { it.ip }
        val agentUpdateIpList = agentUpdateList.mapNotNull { it.ip }
        val agentUpdateHostIdList = agentUpdateList.mapNotNull { it.bkHostId }

        val agentUpdateRecords = existNodeIdToAgentVersionMap.filter { (key, value) ->
            agentUpdateIpList.contains(value.ip) || agentUpdateHostIdList.contains(value.bkHostId)
        }.map { (key, value) ->
            UpdateAgentInfo(
                nodeId = key,
                nodeStatus = if (AGENT_NOT_INSTALLED_TAG == ipToAgentUpdateList[value.ip]?.installedTag)
                    NodeStatus.NOT_INSTALLED.name
                else if (AGENT_ABNORMAL_NODE_STATUS == ipToAgentUpdateList[value.ip]?.status)
                    NodeStatus.ABNORMAL.name
                else if (AGENT_NORMAL_NODE_STATUS == ipToAgentUpdateList[value.ip]?.status)
                    NodeStatus.NORMAL.name
                else null,
                agentStatus = AGENT_NORMAL_NODE_STATUS == ipToAgentUpdateList[value.ip]?.status,
                agentVersion = ipToAgentUpdateList[value.ip]?.version,
                lastModifyTime = LocalDateTime.now()
            )
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
            // 2.1 在CC - 改为NORMAL
            val inCCIpList = nodeCCInfoList.mapNotNull { it.bkHostInnerip }
            inCCIpList.takeIf { it.isNotEmpty() }.run {
                nodeDao.updateNodeInCCByIp(dslContext, inCCIpList)
                // 4. CC中信息（host_id和云区域id）改变 - 更新信息，不变 - 不操作
                val nodeUpdateInfoList = nodeRecords.filterNot {
                    it[T_NODE_HOST_ID] as Long == ipToCCInfoMap!![it[T_NODE_NODE_IP] as String]?.bkHostId &&
                        it[T_NODE_CLOUD_AREA_ID] as Long == ipToCCInfoMap!![it[T_NODE_NODE_IP] as String]
                        ?.bkCloudId?.toLong()
                }.takeIf { it.isNotEmpty() }!!.map {
                    HostIdAndCloudAreaIdInfo(
                        nodeId = it[T_NODE_NODE_ID] as Long,
                        bkCloudId = ipToCCInfoMap!![it[T_NODE_NODE_IP] as String]?.bkCloudId?.toLong(),
                        bkHostId = ipToCCInfoMap!![it[T_NODE_NODE_IP] as String]?.bkHostId
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