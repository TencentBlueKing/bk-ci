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
import com.tencent.devops.environment.constant.T_NODE_AGENT_VERSION
import com.tencent.devops.environment.constant.T_NODE_CLOUD_AREA_ID
import com.tencent.devops.environment.constant.T_NODE_HOST_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.constant.T_NODE_NODE_STATUS
import com.tencent.devops.environment.constant.T_NODE_OS_TYPE
import com.tencent.devops.environment.constant.T_NODE_PROJECT_ID
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.job.AgentVersion
import com.tencent.devops.environment.pojo.job.UpdateTNodeInfo
import com.tencent.devops.environment.pojo.job.ccres.CCInfo
import com.tencent.devops.environment.pojo.job.jobreq.OpOperateReq
import com.tencent.devops.environment.pojo.job.jobresp.CCUpdateInfo
import com.tencent.devops.environment.service.CmdbNodeService
import com.tencent.devops.environment.utils.ComputeTimeUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service("TencentStockDataUpdateService")
@Primary
class TencentStockDataUpdateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val cmdbNodeDao: CmdbNodeDao,
    private val tencentQueryFromCmdbService: TencentQueryFromCmdbService,
    private val queryFromCCService: QueryFromCCService,
    private val cmdbNodeService: CmdbNodeService,
    private val redisOperation: RedisOperation,
    private val queryAgentStatusService: QueryAgentStatusService,
    private val opService: OpService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TencentStockDataUpdateService::class.java)

        private const val ADD_NODES_TO_CC_TIMEOUT_LOCK_KEY = "add_nodes_to_cc_timeout_lock"
        private const val SCHEDULED_CHECK_NODES_TIMEOUT_LOCK_KEY = "scheduled_check_nodes_timeout_lock"
        private const val SCHEDULED_UPDATE_GSE_AGENT_TIMEOUT_LOCK_KEY = "scheduled_update_gse_agent_timeout_lock"

        private const val DEFAULT_PAGE_SIZE = 100
        private const val EXPIRATION_TIME_OF_THE_LOCK = 600L

        const val AGENT_ABNORMAL_NODE_STATUS = 0
        const val AGENT_NORMAL_NODE_STATUS = 1
        const val AGENT_NOT_INSTALLED_TAG = false

        const val FIELD_BK_OS_TYPE = "bk_os_type"
        const val FIELD_BK_HOST_ID = "bk_host_id"
        const val FIELD_BK_CLOUD_ID = "bk_cloud_id"
        const val FIELD_BK_HOST_INNERIP = "bk_host_innerip"

        const val FIRST_IP_INDEX = 0
    }

    /**
     * checkDeployNodesIsInCmdb:
     * 后台定时轮询机器状态，看机器在不在公司cmdb中
     * 轮询T_NODE表中 NODE_TYPE==部署 的记录。部署：CMDB("CMDB")，UNKNOWN("未知")，OTHER("其他")
     * 在不在cmdb -> 在不在cc -> cc的host_id和云区域id是否改变
     * cron：每小时 第7,37分钟 各执行一次。
     */
    @Scheduled(cron = "0 7,37 * * * ?")
    fun checkDeployNodes() {
        taskWithRedisLock(SCHEDULED_CHECK_NODES_TIMEOUT_LOCK_KEY, ::checkDeployNodesIsInCmdb)
    }

    /**
     * updateGseAgent:
     * 定时任务：gse agent状态/版本 轮询 + 差量更新
     * 条件：NODE_TYPE为"部署"的，查询该节点的agent安装状态以及版本，并对比差异更新。
     * 分组执行，每次遍历1000条记录。
     * cron：每小时 第8,38分钟 各执行一次。
     */
    @Scheduled(cron = "0 8,38 * * * ?")
    fun scheduledUpdateGseAgent() {
        taskWithRedisLock(SCHEDULED_UPDATE_GSE_AGENT_TIMEOUT_LOCK_KEY, ::updateGseAgent)
    }

    /**
     * addNodeToCC:
     * 分组执行，每次遍历100条记录。
     * 将不在CC中的 类型为CMDB 的节点，添加到CC中，并返回host_id、云区域id和操作系统类型，将host_id、云区域id和操作系统类型写入表中
     * 存量数据更新任务：执行一次。
     * 提供apigw接口
     */
    fun addNodesToCCOnce() {
        taskWithRedisLock(ADD_NODES_TO_CC_TIMEOUT_LOCK_KEY, ::addNodesToCC)
    }

    private fun checkDeployNodesIsInCmdb() {
        logger.info("Check deploy nodes are in cmdb task starts...")
        val startTime = LocalDateTime.now()
        val countNodeInCmdb = cmdbNodeDao.countDeployNodes(dslContext)
        logger.info("Node(s) count in cmdb: $countNodeInCmdb")
        countNodeInCmdb.takeIf { it > 0 }.run {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countNodeInCmdb.toLong())
            val time1 = LocalDateTime.now()
            for (page in 1..totalPages) {
                checkDeployNodesIsInCmdbByPage(page)
            }
            logger.info(
                "[checkDeployNodesIsInCmdb]total time: " +
                    "${ComputeTimeUtils.calculateDuration(startTime, LocalDateTime.now())}s, " +
                    "check deploy nodes are in cmdb time: " +
                    "${ComputeTimeUtils.calculateDuration(time1, LocalDateTime.now())}s"
            )
        }
        // 2.2 节点在cmdb中，查询CC: 在CC-改为NORMAL，不在CC-改为NOT_IN_CC
        checkDeployNodesIsInCC()
        logger.info("End Check whether the node is in the cmdb.")
    }

    private fun checkDeployNodesIsInCmdbByPage(page: Int) {
        // 1. 节点：类型为部署："CMDB"，"UNKNOW"，"OTHER"
        val cmdbNodesRecords = cmdbNodeDao.getDeployNodesLimit(dslContext, page, DEFAULT_PAGE_SIZE)
        // 节点ip
        val nodeIpList = cmdbNodesRecords.map { it[T_NODE_NODE_IP] as String }.toSet()
        // 节点：ip - cmdb record（从cmdb查到的，节点在cmdb中）
        val ipToCmdbInfoMap = tencentQueryFromCmdbService.queryCmdbInfoFromIp(nodeIpList)
        // 2.1 不在cmdb中，置空 host_id 和 云区域id, 对应节点的 NODE_STATUS字段 要改成 NOT_IN_CMDB
        val invalidIpList = nodeIpList.filterNot { ipToCmdbInfoMap?.containsKey(it) ?: false }
        cmdbNodeDao.updateNodeNotInCmdb(dslContext, invalidIpList)
    }

    /**
     * checkDeployNodesIsInCC:
     * 后台定时轮询机器状态，看机器是否在CC中
     * 轮询T_NODE表中 NODE_TYPE为"部署"的记录。部署：CMDB("CMDB")，UNKNOWN("未知")，OTHER("其他")
     * 在不在cc -> cc的host_id和云区域id是否改变
     * cron：每小时执行一次。
     */
    private fun checkDeployNodesIsInCC() {
        logger.info("Check deploy nodes are in cc task starts...")
        val startTime = LocalDateTime.now()
        val countHostIdNotNullRecord = cmdbNodeDao.countDeployNodesInCmdb(dslContext)
        logger.info("Check deploy node(s) is in CC, node(s) count:$countHostIdNotNullRecord.")
        countHostIdNotNullRecord.takeIf { it > 0 }.run {
            val totalPagesHostIdNotNull = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countHostIdNotNullRecord.toLong())
            val time1 = LocalDateTime.now()
            for (pageHostIdNotNull in 1..totalPagesHostIdNotNull) {
                checkDeployNodesIsInCCByPage(pageHostIdNotNull)
            }
            logger.info(
                "[checkDeployNodesIsInCC]total time: " +
                    "${ComputeTimeUtils.calculateDuration(startTime, LocalDateTime.now())}s, " +
                    "check deploy nodes are in cc time: " +
                    "${ComputeTimeUtils.calculateDuration(time1, LocalDateTime.now())}s"
            )
        }
    }

    private fun checkDeployNodesIsInCCByPage(page: Int) {
        // 1. 节点record："部署"类型
        val nodeRecords = cmdbNodeDao.getDeployNodesInCmdbLimit(dslContext, page, DEFAULT_PAGE_SIZE)
        // 要判断在不在cc中的 所有节点ip
        val nodeIpList = nodeRecords.map { it[T_NODE_NODE_IP] as String }.toSet()
        // cc记录
        val nodeCCInfoList = if (nodeIpList.isNotEmpty()) {
            queryFromCCService.queryCCListHostWithoutBizByInRules(
                listOf(FIELD_BK_HOST_INNERIP, FIELD_BK_HOST_ID, FIELD_BK_CLOUD_ID, FIELD_BK_OS_TYPE),
                nodeIpList,
                QueryFromCCService.FIELD_BK_HOST_INNERIP
            ).data?.info
        } else null
        var ipToCCInfoMap: Map<String?, CCInfo> = mapOf()
        if (!nodeCCInfoList.isNullOrEmpty()) {
            // ip - cc记录 映射
            ipToCCInfoMap = nodeCCInfoList.associateBy { it.bkHostInnerip?.split(",")?.get(FIRST_IP_INDEX) }
            // 2.1 在CC - 查询节点agent状态并更新
            val inCCIpList = nodeCCInfoList.mapNotNull { it.bkHostInnerip?.split(",")?.get(FIRST_IP_INDEX) }
            if (inCCIpList.isNotEmpty()) {
                val ipToAgentVersionInfoMap = queryAgentStatusService.getAgentVersions(
                    nodeCCInfoList.map {
                        AgentVersion(ip = it.bkHostInnerip?.split(",")?.get(FIRST_IP_INDEX), bkHostId = it.bkHostId)
                    }
                )?.associateBy { it.ip }
                val ipToNodeStatus = mutableMapOf<String, String>()
                inCCIpList.map { ipToNodeStatus[it] = getNodeStatus(ipToAgentVersionInfoMap?.get(it)) }
                cmdbNodeDao.updateNodeInCCByIp(dslContext, ipToNodeStatus)
                // 4. CC中信息（host_id、云区域id、操作系统类型）改变 - 更新信息，不变 - 不操作
                val nodeUpdateInfoList = nodeRecords.filterNot {
                    it[T_NODE_HOST_ID] as? Long == ipToCCInfoMap[it[T_NODE_NODE_IP] as String]?.bkHostId &&
                        it[T_NODE_CLOUD_AREA_ID] as? Long == ipToCCInfoMap[it[T_NODE_NODE_IP] as String]
                        ?.bkCloudId?.toLong() &&
                        it[T_NODE_OS_TYPE] as? String == cmdbNodeService.getOsTypeByCCCode(
                        ipToCCInfoMap[it[T_NODE_NODE_IP] as String]?.osType
                    )
                }.takeIf { it.isNotEmpty() }?.map {
                    CCUpdateInfo(
                        nodeId = it[T_NODE_NODE_ID] as Long,
                        bkCloudId = ipToCCInfoMap[it[T_NODE_NODE_IP] as String]?.bkCloudId?.toLong(),
                        bkHostId = ipToCCInfoMap[it[T_NODE_NODE_IP] as String]?.bkHostId,
                        osType = cmdbNodeService.getOsTypeByCCCode(ipToCCInfoMap[it[T_NODE_NODE_IP] as String]?.osType)
                    )
                }
                if (!nodeUpdateInfoList.isNullOrEmpty()) {
                    cmdbNodeDao.updateHostIdAndCloudAreaIdByNodeId(dslContext, nodeUpdateInfoList)
                }
            }
        }
        // 2.2 不在cc中: 置空 host_id、云区域id、操作系统类型、agent版本，且 NODE_STATUS 改成 NOT_IN_CC
        val invalidIpList = nodeIpList.filterNot { ipToCCInfoMap.containsKey(it) }
        if (invalidIpList.isNotEmpty()) {
            cmdbNodeDao.updateNodeNotInCCByIp(dslContext, invalidIpList)
        }
    }

    private fun updateGseAgent() {
        val countCmdbNodes = cmdbNodeDao.countCmdbNodes(dslContext)
        logger.info("Update gse agent, node(s) quantity: $countCmdbNodes.")
        countCmdbNodes.takeIf { it > 0 }?.run {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countCmdbNodes.toLong())
            for (page in 1..totalPages) {
                val cmdbNodesRecords = cmdbNodeDao.getCmdbNodes(dslContext, page, DEFAULT_PAGE_SIZE)
                val existNodeIdToAgentVersionMap = cmdbNodesRecords.filter {
                    val opInfo = opService.operateOpProject(
                        "", OpOperateReq(2, listOf(it[T_NODE_PROJECT_ID] as String))
                    ).projGrayStatus?.get(0)
                    it[T_NODE_PROJECT_ID] as String == opInfo?.englishName && true == opInfo.projGrayStatus
                }.associate {
                    it[T_NODE_NODE_ID] as Long to
                        AgentVersion(
                            ip = it[T_NODE_NODE_IP] as? String,
                            bkHostId = it[T_NODE_HOST_ID] as? Long,
                            installedTag = NodeStatus.NOT_INSTALLED.name != it[T_NODE_NODE_STATUS] as String,
                            version = it[T_NODE_AGENT_VERSION] as? String,
                            status = if (NodeStatus.NORMAL.name == it[T_NODE_NODE_STATUS] as String) 1 else 0
                        )
                }
                val existAgentVersionList = existNodeIdToAgentVersionMap.values.toList()
                if (logger.isDebugEnabled)
                    logger.debug(
                        "[updateGseAgent]existAgentVersionList:" +
                            existAgentVersionList.joinToString(separator = ", ", transform = { it.toString() })
                    )
                val hostIdToExistAgentVersion = existAgentVersionList.groupBy { it.bkHostId }
                val newAgentVersionList = queryAgentStatusService.getAgentVersions(existAgentVersionList)
                if (logger.isDebugEnabled)
                    logger.debug(
                        "[updateGseAgent]newAgentVersionList:" +
                            newAgentVersionList?.joinToString(separator = ", ", transform = { it.toString() })
                    )
                // 判断 newAgentVersionList 和 existAgentVersionList 是否一致，不一致则更新对应数据库表
                val agentUpdateList = newAgentVersionList?.filterNot {
                    hostIdToExistAgentVersion[it.bkHostId]?.all { item ->
                        // 1. 判断一个分组（同一个hostId）中的AgentVersion是否相同，不同则统一重新重新写入
                        item.installedTag == hostIdToExistAgentVersion[it.bkHostId]?.get(0)?.installedTag &&
                            item.version == hostIdToExistAgentVersion[it.bkHostId]?.get(0)?.version &&
                            item.status == hostIdToExistAgentVersion[it.bkHostId]?.get(0)?.status
                    } ?: false && (
                        // 2. 这个分组（同一个hostId）的AgentVersion相同，判断第一个元素的值，新查的和db中的是否相同，不同则更新
                        it.installedTag == hostIdToExistAgentVersion[it.bkHostId]?.get(0)?.installedTag &&
                            it.version == hostIdToExistAgentVersion[it.bkHostId]?.get(0)?.version &&
                            it.status == hostIdToExistAgentVersion[it.bkHostId]?.get(0)?.status
                        )
                }
                logger.info(
                    "[updateGseAgent]agentUpdateList:" +
                        agentUpdateList?.joinToString(separator = ", ", transform = { it.toString() })
                )
                if (!agentUpdateList.isNullOrEmpty()) {
                    batchUpdateAgent(existNodeIdToAgentVersionMap, agentUpdateList)
                }
            }
        }
    }

    private fun batchUpdateAgent(
        existNodeIdToAgentVersionMap: Map<Long, AgentVersion>,
        agentUpdateList: List<AgentVersion>
    ) {
        val hostIdToAgentUpdateList = agentUpdateList.associateBy { it.bkHostId }
        val agentUpdateIpList = agentUpdateList.mapNotNull { it.ip }
        val agentUpdateHostIdList = agentUpdateList.mapNotNull { it.bkHostId }
        val agentUpdateRecords = existNodeIdToAgentVersionMap.filter { (key, value) ->
            agentUpdateIpList.contains(value.ip) || agentUpdateHostIdList.contains(value.bkHostId)
        }.map { (key, value) ->
            UpdateTNodeInfo(
                nodeId = key,
                nodeStatus = getNodeStatus(hostIdToAgentUpdateList[value.bkHostId]),
                agentStatus = AGENT_NORMAL_NODE_STATUS == hostIdToAgentUpdateList[value.bkHostId]?.status,
                agentVersion = hostIdToAgentUpdateList[value.bkHostId]?.version,
                lastModifyTime = LocalDateTime.now()
            )
        }
        if (logger.isDebugEnabled)
            logger.debug(
                "[batchUpdateAgent]agentUpdateRecords:" +
                    agentUpdateRecords.joinToString(separator = ", ", transform = { it.toString() })
            )
        cmdbNodeDao.batchUpdateAgentInfo(dslContext, agentUpdateRecords)
    }

    private fun addNodesToCC() {
        val countCmdbNodes = cmdbNodeDao.countCmdbNodes(dslContext)
        logger.info("Add to cc cmdb node(s) quantity: $countCmdbNodes")
        countCmdbNodes.takeIf { it > 0 }?.run {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countCmdbNodes.toLong())
            for (page in 1..totalPages) {
                addNodeToCCByPage(page)
            }
        }
    }

    private fun addNodeToCCByPage(page: Int) {
        val cmdbNodesRecords =
            cmdbNodeDao.getCmdbNodesHostIdNullLimit(dslContext, page, DEFAULT_PAGE_SIZE) // 所有"部署"节点 record
        val cmdbNodesIp = cmdbNodesRecords.map { it[T_NODE_NODE_IP] as String }.toSet() // 所有"部署"节点 ip
        val nodeIpToNodesRecords = cmdbNodesRecords.associateBy { it[T_NODE_NODE_IP] as String } // 所有"部署"节点 ip - record
        val ipToCmdbInfoMap = tencentQueryFromCmdbService.queryCmdbInfoFromIp(cmdbNodesIp) // 所有"部署"节点 ip - cmdb信息
        if (!ipToCmdbInfoMap.isNullOrEmpty()) {
            val svrIdToCmdbInfoMap = ipToCmdbInfoMap.values
                .associateBy { it.serverId?.toLong() } // 所有"部署"节点 svrId - cmdb信息
            val svrIdList = ipToCmdbInfoMap.values.mapNotNull { it.serverId?.toLong() } // 所有"部署"节点 svrId
            // 所有"部署"节点 用svrId查询在不在CC中
            val (_, inCCSvrIdList, notInCCSvrIdList) = cmdbNodeService.checkNodeInCCBySvrId(svrIdList)
            // 不在CC中 - 通过节点svrId 添加到CC中，查出host_id和云区域id，写入db对应记录
            if (notInCCSvrIdList.isNotEmpty()) {
                val addToCCResp = queryFromCCService.addHostToCiBiz(notInCCSvrIdList)
                val ccHostIdList = addToCCResp.data?.bkHostIds
                val (notInCCSvrIdQueryCCRes, _, _) = cmdbNodeService.checkNodeInCCBySvrId(notInCCSvrIdList)
                val svrIdQueryCCList = notInCCSvrIdQueryCCRes.data?.info // 所有刚添加到cc中的节点 cc信息
                val hostIdToCCinfo = svrIdQueryCCList?.associateBy { it.bkHostId }
                val addToCCInfoList = ccHostIdList?.mapIndexed { index, value ->
                    CCUpdateInfo(
                        nodeId = nodeIpToNodesRecords[svrIdToCmdbInfoMap[notInCCSvrIdList[index]]?.SvrIp]
                            ?.get(T_NODE_NODE_ID) as Long,
                        bkCloudId = hostIdToCCinfo?.get(value)?.bkCloudId?.toLong(),
                        bkHostId = value,
                        osType = cmdbNodeService.getOsTypeByCCCode(hostIdToCCinfo?.get(value)?.osType)
                    )
                }
                if (!addToCCInfoList.isNullOrEmpty()) {
                    cmdbNodeDao.updateHostIdAndCloudAreaIdByNodeId(dslContext, addToCCInfoList)
                }
            }
        }
    }

    private fun getNodeStatus(agentInfo: AgentVersion?): String {
        return if (AgentService.AGENT_NOT_INSTALLED_TAG == agentInfo?.installedTag)
            NodeStatus.NOT_INSTALLED.name
        else if (AgentService.AGENT_ABNORMAL_NODE_STATUS == agentInfo?.status)
            NodeStatus.ABNORMAL.name
        else if (AgentService.AGENT_NORMAL_NODE_STATUS == agentInfo?.status)
            NodeStatus.NORMAL.name
        else
            NodeStatus.NOT_INSTALLED.name
    }

    private fun taskWithRedisLock(lockKey: String, operation: () -> Unit) {
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
            logger.info("[taskWithRedisLock]Unlocked.")
        }
    }
}