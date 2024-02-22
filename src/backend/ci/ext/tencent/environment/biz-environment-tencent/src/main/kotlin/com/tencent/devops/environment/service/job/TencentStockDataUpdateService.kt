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
import com.tencent.devops.environment.constant.T_NODE_AGENT_STATUS
import com.tencent.devops.environment.constant.T_NODE_AGENT_VERSION
import com.tencent.devops.environment.constant.T_NODE_HOST_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.constant.T_NODE_NODE_STATUS
import com.tencent.devops.environment.constant.T_NODE_PROJECT_ID
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.OsType
import com.tencent.devops.environment.pojo.job.AgentVersion
import com.tencent.devops.environment.pojo.job.CCUpdateInfo
import com.tencent.devops.environment.pojo.job.UpdateTNodeInfo
import com.tencent.devops.environment.pojo.job.req.OpOperateReq
import com.tencent.devops.environment.service.CmdbNodeService
import com.tencent.devops.environment.service.CmdbNodeService.Companion.OS_TYPE_CC_CODE_AIX
import com.tencent.devops.environment.service.CmdbNodeService.Companion.OS_TYPE_CC_CODE_LINUX
import com.tencent.devops.environment.service.CmdbNodeService.Companion.OS_TYPE_CC_CODE_WINDOWS
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
    private val nodeDao: NodeDao,
    private val tencentQueryFromCmdbService: TencentQueryFromCmdbService,
    private val queryFromCCService: QueryFromCCService,
    private val cmdbNodeService: CmdbNodeService,
    private val queryAgentStatusService: QueryAgentStatusService,
    private val opService: OpService,
    private val stockDataUpdateService: StockDataUpdateService
) : IStockDataUpdateService {

    companion object {
        private val logger = LoggerFactory.getLogger(TencentStockDataUpdateService::class.java)
        private const val SCHEDULED_UPDATE_GSE_AGENT_TIMEOUT_LOCK_KEY = "scheduled_update_gse_agent_timeout_lock"
        private const val ADD_NODES_TO_CC_TIMEOUT_LOCK_KEY = "add_nodes_to_cc_timeout_lock"
        private const val DEFAULT_PAGE_SIZE = 100

        private const val AGENT_NOT_INSTALLED_TAG = false
        private const val AGENT_ABNORMAL_NODE_STATUS = 0
        private const val AGENT_NORMAL_NODE_STATUS = 1
    }

    /**
     * checkDeployNodesIsInCmdb:
     * 后台定时轮询机器状态，看机器在不在公司cmdb中
     * 轮询T_NODE表中 NODE_TYPE==部署 的记录。部署：CMDB("CMDB")，UNKNOWN("未知")，OTHER("其他")
     * 在不在cmdb -> 在不在cc -> cc的host_id和云区域id是否改变
     * cron：每小时执行一次。
     */
    override fun checkDeployNodes() {
        checkDeployNodesIsInCmdb()
    }

    /**
     * updateAgent:
     * 定时任务：gse agent状态/版本 轮询 + 差量更新
     * 条件：NODE_TYPE为"部署"的，查询该节点的agent安装状态以及版本，并对比差异更新。
     * 同步更新蓝盾agent。
     * 分组执行，每次遍历1000条记录。
     * cron：每小时执行一次。
     */
    @Scheduled(cron = "0 17 * * * ?")
    fun scheduledUpdateGseAgent() {
        stockDataUpdateService.taskWithRedisLock(SCHEDULED_UPDATE_GSE_AGENT_TIMEOUT_LOCK_KEY, ::updateGseAgent)
    }

    /**
     * addNodeToCC:
     * 分组执行，每次遍历100条记录。
     * 将不在CC中的 类型为CMDB 的节点，添加到CC中，并返回host_id和云区域id，将host_id和云区域id写入表中
     * 存量数据更新任务：执行一次。
     */
    fun addNodesToCCOnce() {
        stockDataUpdateService.taskWithRedisLock(ADD_NODES_TO_CC_TIMEOUT_LOCK_KEY, ::addNodesToCC)
    }

    private fun updateGseAgent() {
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
                            ip = it[T_NODE_NODE_IP] as? String,
                            bkHostId = it[T_NODE_HOST_ID] as? Long,
                            installedTag = NodeStatus.NOT_INSTALLED.name != it[T_NODE_NODE_STATUS] as String,
                            version = it[T_NODE_AGENT_VERSION] as? String,
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
            UpdateTNodeInfo(
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
        nodeDao.batchUpdateAgentInfo(dslContext, agentUpdateRecords)
    }

    private fun addNodesToCC() {
        val countCmdbNodes = nodeDao.countCmdbNodes(dslContext)
        if (logger.isDebugEnabled) logger.debug("[addNodeToCC]countCmdbNodes:$countCmdbNodes")
        countCmdbNodes.takeIf { it > 0 }?.run {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countCmdbNodes.toLong())
            for (page in 1..totalPages) {
                addNodeToCCByPage(page)
            }
        }
    }

    private fun addNodeToCCByPage(page: Int) {
        // 所有"部署"节点 record
        val cmdbNodesRecords =
            nodeDao.getCmdbNodesHostIdNullLimit(dslContext, page - 1, DEFAULT_PAGE_SIZE)
        if (logger.isDebugEnabled) logger.debug("[addNodeToCC]cmdbNodesRecords:$cmdbNodesRecords")
        // 所有"部署"节点 ip
        val cmdbNodesIp = cmdbNodesRecords.map { it[T_NODE_NODE_IP] as String }.toSet()
        if (logger.isDebugEnabled) logger.debug("[addNodeToCC]cmdbNodesIp:$cmdbNodesIp.")
        // 所有"部署"节点 ip - record
        val nodeIpToNodesRecords = cmdbNodesRecords.associateBy { it[T_NODE_NODE_IP] as String }
        // 所有"部署"节点 ip - cmdb信息
        val ipToCmdbInfoMap = tencentQueryFromCmdbService.queryCmdbInfoFromIp(cmdbNodesIp)
        if (logger.isDebugEnabled) logger.debug("[addNodeToCC]ipToCmdbInfoMap:$ipToCmdbInfoMap.")
        ipToCmdbInfoMap.takeIf { !it.isNullOrEmpty() }.run {
            // 所有"部署"节点 svrId - cmdb信息
            val svrIdToCmdbInfoMap = ipToCmdbInfoMap!!.values
                .associateBy { it.serverId?.toLong() }
            if (logger.isDebugEnabled) logger.debug("[addNodeToCC]svrIdToCmdbInfoMap:$svrIdToCmdbInfoMap.")
            // 所有"部署"节点 svrId
            val svrIdList = ipToCmdbInfoMap.values.mapNotNull { it.serverId?.toLong() }
            // 所有"部署"节点 用svrId查询在不在CC中
            val (_, inCCSvrIdList, notInCCSvrIdList) = cmdbNodeService.checkNodeInCCBySvrId(svrIdList)
            if (logger.isDebugEnabled) logger.debug("[addNodeToCC]inCCSvrIdList:$inCCSvrIdList.")
            if (logger.isDebugEnabled) logger.debug("[addNodeToCC]notInCCSvrIdList:$notInCCSvrIdList.")
            // 不在CC中 - 通过节点svrId 添加到CC中，查出host_id和云区域id，写入db对应记录
            val addToCCResp = queryFromCCService.addHostToCiBiz(notInCCSvrIdList)
            if (logger.isDebugEnabled) logger.debug("[addNodeToCC]addToCCResp:$addToCCResp")
            val ccHostIdList = addToCCResp.data?.bkHostIds
            val (svrIdQueryCCRes, _, _) = cmdbNodeService.checkNodeInCCBySvrId(notInCCSvrIdList)
            val svrIdQueryCCList = svrIdQueryCCRes.data?.info // 所有刚添加到cc中的节点 cc信息
            val hostIdToCCinfo = svrIdQueryCCList?.associateBy { it.bkHostId }
            val addToCCInfoList = ccHostIdList?.mapIndexed { index, value ->
                val osType = when (hostIdToCCinfo?.get(value)?.osType) {
                    OS_TYPE_CC_CODE_LINUX -> OsType.LINUX.name
                    OS_TYPE_CC_CODE_WINDOWS -> OsType.WINDOWS.name
                    OS_TYPE_CC_CODE_AIX -> OsType.AIX.name
                    else -> OsType.OTHER.name
                }
                CCUpdateInfo(
                    nodeId = nodeIpToNodesRecords[svrIdToCmdbInfoMap[notInCCSvrIdList[index]]?.SvrIp]
                        ?.get(T_NODE_NODE_ID) as Long,
                    bkCloudId = hostIdToCCinfo?.get(value)?.bkCloudId?.toLong(),
                    bkHostId = value,
                    osType = osType
                )
            }
            if (!addToCCInfoList.isNullOrEmpty()) {
                nodeDao.updateHostIdAndCloudAreaIdByNodeId(dslContext, addToCCInfoList)
            }
        }
    }

    private fun checkDeployNodesIsInCmdb() {
        val countNodeInCmdb = nodeDao.countDeployNodes(dslContext)
        if (logger.isDebugEnabled) logger.debug("[checkDeployNodesIsInCmdb]countNodeInCmdb:$countNodeInCmdb")
        countNodeInCmdb.takeIf { it > 0 }.run {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, countNodeInCmdb.toLong())
            for (page in 1..totalPages) {
                checkDeployNodesIsInCmdbByPage(page)
            }
        }
        // 2.2 节点在cmdb中，查询CC: 在CC-改为NORMAL，不在CC-改为NOT_IN_CC
        stockDataUpdateService.checkDeployNodesIsInCC()
        if (logger.isDebugEnabled) logger.debug("[checkDeployNodesIsInCmdb]End Check whether the node is in the cmdb.")
    }

    private fun checkDeployNodesIsInCmdbByPage(page: Int) {
        // 1. 节点：类型为部署："CMDB"，"UNKNOW"，"OTHER"
        val cmdbNodesRecords = nodeDao.getDeployNodesLimit(dslContext, page, DEFAULT_PAGE_SIZE)
        // 节点ip
        val nodeIpList = cmdbNodesRecords.map { it[T_NODE_NODE_IP] as String }.toSet()
        // 节点：ip - cmdb record（从cmdb查到的，节点在cmdb中）
        val ipToCmdbInfoMap = tencentQueryFromCmdbService.queryCmdbInfoFromIp(nodeIpList)
        // 2.1 不在cmdb中，置空 host_id 和 云区域id, 对应节点的 NODE_STATUS字段 要改成 NOT_IN_CMDB
        val invalidIpList = nodeIpList.filterNot { ipToCmdbInfoMap?.containsKey(it) ?: false }
        nodeDao.updateNodeNotInCmdb(dslContext, invalidIpList)
    }
}