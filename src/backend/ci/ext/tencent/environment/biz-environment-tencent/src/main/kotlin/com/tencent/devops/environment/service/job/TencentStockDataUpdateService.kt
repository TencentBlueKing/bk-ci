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
import com.tencent.devops.environment.constant.Constants.FIELD_BK_CLOUD_ID
import com.tencent.devops.environment.constant.Constants.FIELD_BK_HOST_ID
import com.tencent.devops.environment.constant.Constants.FIELD_BK_HOST_INNERIP
import com.tencent.devops.environment.constant.Constants.FIELD_BK_OS_TYPE
import com.tencent.devops.environment.constant.Constants.FIELD_BK_SVR_ID
import com.tencent.devops.environment.constant.T_NODE_AGENT_VERSION
import com.tencent.devops.environment.constant.T_NODE_BAK_OPERATOR
import com.tencent.devops.environment.constant.T_NODE_CLOUD_AREA_ID
import com.tencent.devops.environment.constant.T_NODE_HOST_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.constant.T_NODE_NODE_STATUS
import com.tencent.devops.environment.constant.T_NODE_OPERATOR
import com.tencent.devops.environment.constant.T_NODE_OS_NAME
import com.tencent.devops.environment.constant.T_NODE_OS_TYPE
import com.tencent.devops.environment.constant.T_NODE_SERVER_ID
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.dao.job.JobDao
import com.tencent.devops.environment.pojo.dto.NodeUpdateAttrDTO
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.job.AgentVersion
import com.tencent.devops.environment.pojo.job.UpdateTNodeInfo
import com.tencent.devops.environment.pojo.job.ccres.CCInfo
import com.tencent.devops.environment.pojo.job.jobresp.CCUpdateInfo
import com.tencent.devops.environment.service.CmdbNodeService
import com.tencent.devops.environment.service.cmdb.TencentCmdbService
import com.tencent.devops.environment.service.gseagent.GSEAgentService
import com.tencent.devops.environment.utils.ComputeTimeUtils
import org.apache.commons.io.ThreadUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service("TencentStockDataUpdateService")
@Primary
class TencentStockDataUpdateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val cmdbNodeDao: CmdbNodeDao,
    private val jobDao: JobDao,
    private val tencentCmdbService: TencentCmdbService,
    private val tencentQueryFromCCService: TencentQueryFromCCService,
    private val cmdbNodeService: CmdbNodeService,
    private val redisOperation: RedisOperation,
    private val queryAgentStatusService: QueryAgentStatusService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TencentStockDataUpdateService::class.java)

        private const val ADD_NODES_TO_CC_TIMEOUT_LOCK_KEY = "add_nodes_to_cc_timeout_lock"
        private const val SCHEDULED_CHECK_NODES_TIMEOUT_LOCK_KEY = "scheduled_check_nodes_timeout_lock"
        private const val SCHEDULED_UPDATE_GSE_AGENT_TIMEOUT_LOCK_KEY = "scheduled_update_gse_agent_timeout_lock"
        private const val WRITE_SERVER_ID_TIMEOUT_LOCK_KEY = "write_server_id_timeout_lock"
        private const val CLEAR_EXPIRED_JOB_TASK_TIMEOUT_LOCK_KEY = "clear_expired_job_task_timeout_lock"

        private const val DEFAULT_PAGE_SIZE = 100
        private const val EXPIRATION_TIME_OF_THE_LOCK = 3600L

        const val AGENT_NORMAL_NODE_STATUS = 1

        const val FIRST_IP_INDEX = 0

        const val JOB_TASK_EXPIRED_DAYS = 30L
    }

    /**
     * clearExpiredJobTask：
     * 清理T_PROJECT_JOB表的过期数据（job任务过期时间：1个月）
     * cron：每天执行一次
     */
    @Scheduled(cron = "0 19 19 * * ?")
    fun scheduledClearExpiredJobTask() {
        taskWithRedisLock(CLEAR_EXPIRED_JOB_TASK_TIMEOUT_LOCK_KEY, ::clearExpiredJobTask)
    }

    /**
     * checkDeployNodesIsInCmdb:
     * 后台定时轮询机器状态，看机器在不在公司cmdb中
     * 轮询T_NODE表中 NODE_TYPE==部署 的记录。部署：CMDB("CMDB")，UNKNOWN("未知")，OTHER("其他")
     * 在不在cmdb -> 在不在cc -> cc的host_id和云区域id是否改变
     * cron：每10分钟执行一次。
     */
    @Scheduled(cron = "0 7/10 * * * ?")
    fun checkDeployNodesInCmdb() {
        taskWithRedisLock(SCHEDULED_CHECK_NODES_TIMEOUT_LOCK_KEY, ::checkDeployNodesIsInCmdb)
    }

    /**
     * updateGseAgent:
     * 定时任务：gse agent状态/版本 轮询 + 差量更新
     * 条件：NODE_TYPE为"部署"的，查询该节点的agent安装状态以及版本，并对比差异更新。
     * 分组执行，每次遍历1000条记录。
     * cron：每10分钟执行一次。
     */
    @Scheduled(cron = "0 8/10 * * * ?")
    fun updateGseAgentStatusAndVersionPeriodically() {
        taskWithRedisLock(SCHEDULED_UPDATE_GSE_AGENT_TIMEOUT_LOCK_KEY, ::updateGseAgentStatusAndVersion)
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

    /**
     * writeServerId：
     * 分组执行，每次遍历100条记录。
     * 通过ip查询节点对应的server_id，写入数据库。
     * 执行一次，提供apigw接口
     */
    fun writeServerIdOnce() {
        taskWithRedisLock(WRITE_SERVER_ID_TIMEOUT_LOCK_KEY, ::writeServerId)
    }

    private fun clearExpiredJobTask() {
        var deletedRowNum: Int
        var totalDeletedRowNum = 0
        do {
            deletedRowNum = jobDao.deleteExpiredJobTaskRecord(dslContext, JOB_TASK_EXPIRED_DAYS)
            ThreadUtils.sleep(Duration.ofMillis(1000))
            totalDeletedRowNum += deletedRowNum
        } while (deletedRowNum > 0)
        logger.info("{} expired job task(s) deleted.", totalDeletedRowNum)
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
        // 1. 节点：类型为部署："CMDB"，"UNKNOWN"，"OTHER"
        val cmdbNodesRecords = cmdbNodeDao.getDeployNodesLimit(dslContext, page, DEFAULT_PAGE_SIZE)
        // 节点serverId（只更新有serverId的部署节点，正常情况下，部署节点记录都应该有serverId）
        val nodeServerIdSet = cmdbNodesRecords.mapNotNull { it[T_NODE_SERVER_ID] as? Long }.toSet()
        // 节点：cmdb服务器信息（从cmdb查到的，节点在cmdb中）
        val serverIdToCmdbServerMap = tencentCmdbService.queryServerByServerId(nodeServerIdSet)

        // 定时更新(当有变更时)：节点主备负责人、操作系统名称
        val nodeAttrList = cmdbNodesRecords.filter {
            null != it[T_NODE_SERVER_ID]
        }.mapNotNull {
            val serverId = it[T_NODE_SERVER_ID] as Long
            val operatorNew = serverIdToCmdbServerMap[serverId]?.operator
            val bakOperatorNew = serverIdToCmdbServerMap[serverId]?.getBakOperatorStr()
            val osNameNew = serverIdToCmdbServerMap[serverId]?.osName
            if (
                it[T_NODE_OPERATOR] != operatorNew ||
                it[T_NODE_BAK_OPERATOR] != bakOperatorNew ||
                it[T_NODE_OS_NAME] != osNameNew
            ) {
                NodeUpdateAttrDTO(
                    serverId = serverId,
                    operator = operatorNew,
                    bakOperator = bakOperatorNew,
                    osName = osNameNew
                )
            } else null
        }
        cmdbNodeDao.batchUpdateNodeMaintainerAndOsNameByServerId(dslContext, nodeAttrList)
        // 2.1.1 不在cmdb中，置空 host_id 和 云区域id, 对应节点的 NODE_STATUS字段 改成 NOT_IN_CMDB
        val invalidServerIdList = nodeServerIdSet.filterNot { serverIdToCmdbServerMap.containsKey(it) }
        cmdbNodeDao.updateNodeNotInCmdbByServerIdList(dslContext, invalidServerIdList)
        // 2.1.2 在CMDB中，但是节点状态是NOT_IN_CMDB，此类节点，此处更新为NOT_IN_CC，后面在checkDeployNodesIsInCC函数中再进一步更新
        // （正常不应出现这种情况，该逻辑是为防止CMDB接口返回的数据不稳定，导致将一些在CMDB中的节点更新为NOT_IN_CMDB）
        val inCmdbServerIdList = serverIdToCmdbServerMap.keys
        if (inCmdbServerIdList.isNotEmpty()) {
            cmdbNodeDao.updateStatusIncorrectNodeByServerIdList(dslContext, inCmdbServerIdList)
        }
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
        // 1. 节点record："部署"类型，且在CMDB
        val nodeRecords = cmdbNodeDao.getDeployNodesInCmdbLimit(dslContext, page, DEFAULT_PAGE_SIZE)
        // 要判断在不在CC中的所有节点serverId
        val nodeServerIdList = nodeRecords.mapNotNull { it[T_NODE_SERVER_ID] as? Long }.toSet()
        // 通过serverId查询 CC信息
        val nodeCCInfoListFromServerId = if (nodeServerIdList.isNotEmpty()) {
            tencentQueryFromCCService.queryCCListHostWithoutBizByInRules(
                listOf(FIELD_BK_HOST_INNERIP, FIELD_BK_HOST_ID, FIELD_BK_CLOUD_ID, FIELD_BK_OS_TYPE, FIELD_BK_SVR_ID),
                nodeServerIdList,
                FIELD_BK_SVR_ID
            ).data?.info
        } else null
        var serverIdToCCInfoMap: Map<Long?, CCInfo> = mapOf()
        if (!nodeCCInfoListFromServerId.isNullOrEmpty()) {
            // serverId - cc记录 map
            serverIdToCCInfoMap = nodeCCInfoListFromServerId.associateBy { it.svrId }
            // 2.1 在CC - 查询节点agent状态并更新
            val inCCServerIdList = nodeCCInfoListFromServerId.mapNotNull { it.svrId }
            if (inCCServerIdList.isNotEmpty()) {
                val serverIdToAgentVersionInfoMap = queryAgentStatusService.getAgentVersions(
                    nodeCCInfoListFromServerId.filterNot { null == it.svrId }.map {
                        AgentVersion(
                            serverId = it.svrId,
                            ip = it.bkHostInnerip?.split(",")?.get(FIRST_IP_INDEX),
                            bkHostId = it.bkHostId
                        )
                    }
                )?.associateBy { it.serverId }
                val serverIdToNodeStatus = mutableMapOf<Long, String>()
                inCCServerIdList.map {
                    serverIdToNodeStatus[it] = getNodeStatus(serverIdToAgentVersionInfoMap?.get(it))
                }
                cmdbNodeDao.batchUpdateNodeInCCByServerId(dslContext, serverIdToNodeStatus)
                // 4. CC中信息（host_id、云区域id、操作系统类型）改变 - 更新信息，不变 - 不操作
                val nodeUpdateInfoList = nodeRecords.filter {
                    null != it[T_NODE_SERVER_ID]
                }.filterNot {
                    val ccInfo = serverIdToCCInfoMap[it[T_NODE_SERVER_ID] as Long]
                    it[T_NODE_HOST_ID] as? Long == ccInfo?.bkHostId &&
                        it[T_NODE_CLOUD_AREA_ID] as? Long == ccInfo?.bkCloudId?.toLong() &&
                        it[T_NODE_OS_TYPE] as? String == cmdbNodeService.getOsTypeByCCCode(ccInfo?.osType)
                }.takeIf { it.isNotEmpty() }?.map {
                    val ccInfo = serverIdToCCInfoMap[it[T_NODE_SERVER_ID] as Long]
                    CCUpdateInfo(
                        nodeId = it[T_NODE_NODE_ID] as Long,
                        bkCloudId = ccInfo?.bkCloudId?.toLong(),
                        bkHostId = ccInfo?.bkHostId,
                        osType = cmdbNodeService.getOsTypeByCCCode(ccInfo?.osType)
                    )
                }
                if (logger.isDebugEnabled)
                    logger.debug(
                        "[checkDeployNodesIsInCCByPage]nodeUpdateInfoList: ${nodeUpdateInfoList?.joinToString()}"
                    )
                if (!nodeUpdateInfoList.isNullOrEmpty()) {
                    cmdbNodeDao.batchUpdateHostIdAndCloudAreaIdByNodeId(dslContext, nodeUpdateInfoList)
                }
            }
        }
        // 2.2 不在cc中: 置空 host_id、云区域id、agent版本，且 NODE_STATUS 改成 NOT_IN_CC
        val invalidServerIdList = nodeServerIdList.filterNot { serverIdToCCInfoMap.containsKey(it) }
        if (invalidServerIdList.isNotEmpty()) {
            cmdbNodeDao.updateNodeNotInCCByServerId(dslContext, invalidServerIdList)
        }
    }

    private fun updateGseAgentStatusAndVersion() {
        val startTime = LocalDateTime.now()
        val countCmdbNodes = cmdbNodeDao.countCmdbNodes(dslContext)
        logger.info("Update gse agent, node(s) quantity: $countCmdbNodes.")
        countCmdbNodes.takeIf { it > 0 }?.run {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countCmdbNodes.toLong())
            for (page in 1..totalPages) {
                val cmdbNodesRecords = cmdbNodeDao.getCmdbNodes(dslContext, page, DEFAULT_PAGE_SIZE)
                val existNodeIdToAgentVersionMap = cmdbNodesRecords.associate {
                    it[T_NODE_NODE_ID] as Long to
                        AgentVersion(
                            serverId = it[T_NODE_SERVER_ID] as? Long,
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
        logger.info(
            "[updateGseAgent]total time: ${ComputeTimeUtils.calculateDuration(startTime, LocalDateTime.now())}s, "
        )
    }

    private fun batchUpdateAgent(
        existNodeIdToAgentVersionMap: Map<Long, AgentVersion>,
        agentUpdateList: List<AgentVersion>
    ) {
        val hostIdToAgentUpdateList = agentUpdateList.associateBy { it.bkHostId }
        val agentUpdateIpList = agentUpdateList.mapNotNull { it.ip }
        val agentUpdateHostIdList = agentUpdateList.mapNotNull { it.bkHostId }
        val agentUpdateRecords = existNodeIdToAgentVersionMap.filter { (_, value) ->
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
        val cmdbNodesRecords = cmdbNodeDao.getCmdbNodesHostIdNullLimit(dslContext, page, DEFAULT_PAGE_SIZE)
        val cmdbNodeServerIdSet = cmdbNodesRecords.map { it[T_NODE_SERVER_ID] as Long }.toSet()
        val nodeServerIdToNodesRecords = cmdbNodesRecords.associateBy { it[T_NODE_SERVER_ID] as Long }
        val serverIdToCmdbInfoMap = tencentCmdbService.queryServerByServerId(cmdbNodeServerIdSet)

        if (serverIdToCmdbInfoMap.isNotEmpty()) {
            // 所有"部署"节点 用svrId查询在不在CC中
            val (_, _, notInCCSvrIdList) = cmdbNodeService.checkNodeInCCBySvrId(cmdbNodeServerIdSet.toList())
            // 不在CC中 - 通过节点svrId 添加到CC中，查出host_id和云区域id，写入db对应记录
            if (notInCCSvrIdList.isNotEmpty()) {
                val addToCCResp = tencentQueryFromCCService.addHostToCiBiz(notInCCSvrIdList)
                val ccHostIdList = addToCCResp.data?.bkHostIds
                val (notInCCSvrIdQueryCCRes, _, _) = cmdbNodeService.checkNodeInCCBySvrId(notInCCSvrIdList)
                val svrIdQueryCCList = notInCCSvrIdQueryCCRes.data?.info // 所有刚添加到cc中的节点 cc信息
                val hostIdToCCInfo = svrIdQueryCCList?.associateBy { it.bkHostId }
                val addToCCInfoList = ccHostIdList?.mapIndexed { index, value ->
                    CCUpdateInfo(
                        nodeId = nodeServerIdToNodesRecords[serverIdToCmdbInfoMap[notInCCSvrIdList[index]]?.serverId]
                            ?.get(T_NODE_NODE_ID) as Long,
                        bkCloudId = hostIdToCCInfo?.get(value)?.bkCloudId?.toLong(),
                        bkHostId = value,
                        osType = cmdbNodeService.getOsTypeByCCCode(hostIdToCCInfo?.get(value)?.osType)
                    )
                }
                if (!addToCCInfoList.isNullOrEmpty()) {
                    cmdbNodeDao.batchUpdateHostIdAndCloudAreaIdByNodeId(dslContext, addToCCInfoList)
                }
            }
        }
    }

    private fun getNodeStatus(agentInfo: AgentVersion?): String {
        return if (GSEAgentService.AGENT_NOT_INSTALLED_TAG == agentInfo?.installedTag)
            NodeStatus.NOT_INSTALLED.name
        else if (GSEAgentService.AGENT_ABNORMAL_NODE_STATUS == agentInfo?.status)
            NodeStatus.ABNORMAL.name
        else if (GSEAgentService.AGENT_NORMAL_NODE_STATUS == agentInfo?.status)
            NodeStatus.NORMAL.name
        else
            NodeStatus.NOT_INSTALLED.name
    }

    private fun writeServerId() {
        logger.info("Write deploy nodes server id task starts...")
        val startTime = LocalDateTime.now()
        val cmdbNodesCount = cmdbNodeDao.countDeployNodesServerIdNull(dslContext)
        logger.info("Write deploy nodes server id, server id null node(s) count:$cmdbNodesCount.")
        cmdbNodesCount.takeIf { it > 0 }.run {
            val totalPage = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, cmdbNodesCount.toLong())
            val time1 = LocalDateTime.now()
            for (page in 1..totalPage) {
                try {
                    writeServerIdByPage(page)
                } catch (e: Exception) {
                    logger.error("[writeServerId]Error in page[$page], Error:", e)
                }
            }
            logger.info(
                "[writeServerId]total time: ${ComputeTimeUtils.calculateDuration(startTime, LocalDateTime.now())}s, " +
                    "update nodes time: ${ComputeTimeUtils.calculateDuration(time1, LocalDateTime.now())}s"
            )
        }
    }

    private fun writeServerIdByPage(page: Int) {
        // 1. 节点record："部署"类型
        val nodeRecords = cmdbNodeDao.getDeployNodesServerIdNullLimit(dslContext, page, DEFAULT_PAGE_SIZE)
        // 2. 要写入server id的所有节点ip
        val nodeIpSet = nodeRecords.map { it[T_NODE_NODE_IP] as String }.toSet()
        // 3. 请求cmdb，查询serverId，得到：ip - cmdbInfo
        val nodeIpToCmdbServerMap = tencentCmdbService.queryServerByIp(nodeIpSet)
        val nodeIpToServerIdMap = mutableMapOf<String, Long?>()
        nodeIpToCmdbServerMap.forEach { (ip, cmdbInfo) ->
            nodeIpToServerIdMap[ip] = cmdbInfo.serverId
        }
        // 4. 根据ip更新数据库中的部署节点
        if (nodeIpToServerIdMap.isNotEmpty()) {
            cmdbNodeDao.batchUpdateNodeSeverIdByIp(dslContext, nodeIpToServerIdMap)
        }
    }

    private fun taskWithRedisLock(lockKey: String, operation: () -> Unit) {
        val redisLock = RedisLock(redisOperation, lockKey, EXPIRATION_TIME_OF_THE_LOCK)
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("[taskWithRedisLock]Locked. key:$lockKey")
                operation()
            } else {
                logger.info("[taskWithRedisLock]Lock failed. key:$lockKey")
            }
        } catch (e: Throwable) {
            logger.error("[taskWithRedisLock]exception associated with $lockKey, error: ", e)
        } finally {
            redisLock.unlock()
            logger.info("[taskWithRedisLock]Unlocked. key:$lockKey")
        }
    }
}
