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

package com.tencent.devops.environment.service.sync

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.environment.constant.Constants.FIELD_BK_CLOUD_ID
import com.tencent.devops.environment.constant.Constants.FIELD_BK_HOST_ID
import com.tencent.devops.environment.constant.Constants.FIELD_BK_HOST_INNERIP
import com.tencent.devops.environment.constant.Constants.FIELD_BK_OS_TYPE
import com.tencent.devops.environment.constant.Constants.FIELD_BK_SVR_ID
import com.tencent.devops.environment.constant.T_NODE_CLOUD_AREA_ID
import com.tencent.devops.environment.constant.T_NODE_HOST_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_OS_TYPE
import com.tencent.devops.environment.constant.T_NODE_SERVER_ID
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.pojo.dto.NodeUpdateAttrDTO
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.job.AgentVersion
import com.tencent.devops.environment.pojo.job.ccres.CCHost
import com.tencent.devops.environment.pojo.job.jobresp.NodeAttr
import com.tencent.devops.environment.service.CmdbNodeService
import com.tencent.devops.environment.service.cc.TencentCCService
import com.tencent.devops.environment.service.cmdb.TencentCmdbService
import com.tencent.devops.environment.service.gseagent.GSEAgentService
import com.tencent.devops.environment.service.job.QueryAgentStatusService
import com.tencent.devops.environment.utils.ComputeTimeUtils
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service("UpdateCmdbNodeService")
@Primary
class UpdateCmdbNodeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val cmdbNodeDao: CmdbNodeDao,
    private val tencentCmdbService: TencentCmdbService,
    private val tencentQueryFromCCService: TencentCCService,
    private val cmdbNodeService: CmdbNodeService,
    private val queryAgentStatusService: QueryAgentStatusService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UpdateCmdbNodeService::class.java)

        private const val DEFAULT_PAGE_SIZE = 100
        const val FIRST_IP_INDEX = 0
    }

    fun updateCmdbNodeInfo() {
        logger.info("updateCmdbNodeInfo|start")
        val startTime = LocalDateTime.now()
        val cmdbNodeCount = cmdbNodeDao.countDeployNodes(dslContext)
        logger.info("cmdbNodeCount=$cmdbNodeCount")
        // 1.更新节点的公司CMDB状态与属性信息
        cmdbNodeCount.takeIf { it > 0 }.run {
            val totalPages = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, cmdbNodeCount.toLong())
            for (page in 1..totalPages) {
                updateCmdbNodeInfoByPage(page)
            }
        }
        logger.info(
            "updateCmdbNodeInfo|updateCmdbStatus|timeConsuming={}s",
            ComputeTimeUtils.calculateDuration(startTime, LocalDateTime.now())
        )
        // 2.更新节点的CC状态与属性信息
        updateCmdbNodeCCInfo()
        logger.info("updateCmdbNodeInfo|end")
    }

    private fun updateCmdbNodeInfoByPage(page: Int) {
        // 扫描类型为CMDB的所有节点
        val cmdbNodeList = cmdbNodeDao.listCmdbNodes(page, DEFAULT_PAGE_SIZE)
        val nodeIpSet = cmdbNodeList.map { it.nodeIp }.toSet()
        // 查询CMDB服务器信息
        val ipToCmdbServerMap = tencentCmdbService.queryServerByIp(nodeIpSet)

        // 1.在CMDB中存在的节点：更新主备负责人、操作系统名称
        val nodeAttrList = cmdbNodeList.filter {
            StringUtils.isNotBlank(it.nodeIp)
        }.mapNotNull {
            val oldCmdbNode = it
            val newCmdbServer = ipToCmdbServerMap[oldCmdbNode.nodeIp]
            if (oldCmdbNode.operatorOrServerIdOrOsNameChanged(newCmdbServer)) {
                NodeUpdateAttrDTO(
                    nodeIp = oldCmdbNode.nodeIp,
                    serverId = newCmdbServer?.serverId,
                    operator = newCmdbServer?.operator,
                    bakOperator = newCmdbServer?.getBakOperatorStrLessThanMaxLength(),
                    osName = newCmdbServer?.getOsNameLessThanMaxLength()
                )
            } else null
        }
        cmdbNodeDao.batchUpdateNodeMaintainerAndOsNameByNodeIp(nodeAttrList)

        // 2.在CMDB中但节点状态为NOT_IN_CMDB，更新为NOT_IN_CC，后续由其他定时任务进一步更新状态
        val inCmdbNodeIpList = ipToCmdbServerMap.keys
        if (inCmdbNodeIpList.isNotEmpty()) {
            // 找出DB中状态为NOT_IN_CMDB的节点进行更新即可
            val notInCmdbIpListInDB = cmdbNodeDao.listNotInCmdbIps(inCmdbNodeIpList)
            val updatedNum = cmdbNodeDao.updateNotInCmdbToNotInCCByNodeIp(notInCmdbIpListInDB)
            if (updatedNum > 0) {
                logger.info(
                    "updateNotInCmdbToNotInCC|updatedNum={}|notInCmdbIpList={}",
                    updatedNum,
                    notInCmdbIpListInDB
                )
            }
        }

        // 3.不在CMDB中的节点：状态改为NOT_IN_CMDB
        val notInCmdbIpList = nodeIpSet.filterNot { ipToCmdbServerMap.containsKey(it) }
        // 找出DB中状态不为NOT_IN_CMDB的节点进行更新即可
        val statusInCmdbIpList = cmdbNodeDao.listInCmdbIps(notInCmdbIpList)
        if (statusInCmdbIpList.isEmpty()) return
        val affectedRowNum = cmdbNodeDao.updateNodeStatusNotInCmdbByNodeIp(statusInCmdbIpList)
        if (affectedRowNum > 0) {
            logger.info(
                "updateNodeStatusNotInCmdb|affectedRowNum={}|statusInCmdbIpList={}|",
                affectedRowNum,
                statusInCmdbIpList
            )
        }
    }

    /**
     * 后台定时轮询机器状态，看机器是否在CC中
     * 轮询T_NODE表中 NODE_TYPE为"部署"的记录。部署：CMDB("CMDB")，UNKNOWN("未知")，OTHER("其他")
     * 在不在cc -> cc的host_id和云区域id是否改变
     * cron：每小时执行一次。
     */
    private fun updateCmdbNodeCCInfo() {
        logger.info("updateCmdbNodeCCInfo|start")
        val startTime = LocalDateTime.now()
        val inCmdbNodeCount = cmdbNodeDao.countNodeInCmdb()
        logger.info("inCmdbNodeCount=$inCmdbNodeCount")
        inCmdbNodeCount.takeIf { it > 0 }.run {
            val pageCount = PageUtil.calTotalPage(DEFAULT_PAGE_SIZE, inCmdbNodeCount.toLong())
            for (pageNum in 1..pageCount) {
                updateCmdbNodeCCInfoByPage(pageNum)
            }
        }
        logger.info(
            "updateCmdbNodeCCInfo|timeConsuming={}s",
            ComputeTimeUtils.calculateDuration(startTime, LocalDateTime.now())
        )
    }

    private fun updateCmdbNodeCCInfoByPage(page: Int) {
        // 1. 节点record："部署"类型，且在CMDB
        val nodeRecords = cmdbNodeDao.getDeployNodesInCmdbLimit(dslContext, page, DEFAULT_PAGE_SIZE)
        // 要判断在不在CC中的所有节点serverId
        val nodeServerIdList = nodeRecords.mapNotNull { it[T_NODE_SERVER_ID] as? Long }.toSet()
        // 通过serverId查询 CC信息
        val nodeCCInfoListFromServerId = if (nodeServerIdList.isNotEmpty()) {
            tencentQueryFromCCService.listHostsWithoutBiz(
                listOf(FIELD_BK_HOST_INNERIP, FIELD_BK_HOST_ID, FIELD_BK_CLOUD_ID, FIELD_BK_OS_TYPE, FIELD_BK_SVR_ID),
                nodeServerIdList,
                FIELD_BK_SVR_ID
            ).data?.info
        } else null
        var serverIdToCCInfoMap: Map<Long?, CCHost> = mapOf()
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
                    NodeAttr(
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
                    cmdbNodeDao.batchUpdateHostIdAndCloudAreaIdByNodeId(nodeUpdateInfoList)
                }
            }
        }
        // 2.2 不在cc中: 置空 host_id、云区域id、agent版本，且 NODE_STATUS 改成 NOT_IN_CC
        val invalidServerIdList = nodeServerIdList.filterNot { serverIdToCCInfoMap.containsKey(it) }
        if (invalidServerIdList.isNotEmpty()) {
            cmdbNodeDao.updateNodeNotInCCByServerId(dslContext, invalidServerIdList)
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
}

fun main() {
    val map = LinkedHashMap<Long?, String>()
    map[1] = "1"
    map[null] = "2"
    println("hello world")
    println(map)
}
