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
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.pojo.job.HostIdAndCloudAreaIdInfo
import com.tencent.devops.environment.service.CmdbNodeService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service("TencentStockDataUpdateService")
@Primary
class TencentStockDataUpdateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val tencentQueryFromCmdbService: TencentQueryFromCmdbService,
    private val queryFromCCService: QueryFromCCService,
    private val cmdbNodeService: CmdbNodeService,
    private val stockDataUpdateService: StockDataUpdateService
) : IStockDataUpdateService {

    companion object {
        private val logger = LoggerFactory.getLogger(TencentStockDataUpdateService::class.java)
        private const val SCHEDULED_ADD_NODE_TO_CC_TIMEOUT_LOCK_KEY = "scheduled_add_node_to_cc_timeout_lock"
        private const val DEFAULT_PAGE_SIZE = 100
    }

    /**
     * checkDeployNodesIsInCmdb:
     * 后台定时轮询机器状态，看机器在不在公司cmdb中
     * 轮询T_NODE表中 NODE_TYPE==部署 的记录。部署：CMDB("CMDB")，UNKNOWN("未知")，OTHER("其他")
     * 在不在cmdb -> 在不在cc -> cc的host_id和云区域id是否改变
     * cron：每天上午10点执行。
     */
    override fun checkDeployNodes() {
        checkDeployNodesIsInCmdb()
    }

    /**
     * addNodeToCC:
     * 定时任务：执行一次就行。
     * 分组执行，每次遍历100条记录。
     * 将不在CC中的 类型为CMDB 的节点，添加到CC中，并返回host_id和云区域id，将host_id和云区域id写入表中
     */
    fun addNodesToCCOnce() {
        stockDataUpdateService.taskWithRedisLock(SCHEDULED_ADD_NODE_TO_CC_TIMEOUT_LOCK_KEY, ::addNodesToCC)
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
        val cmdbNodesIp = cmdbNodesRecords.map { it.value1() }.toSet()
        if (logger.isDebugEnabled) logger.debug("[addNodeToCC]cmdbNodesIp:$cmdbNodesIp.")
        // 所有"部署"节点 ip - record
        val nodeIpToNodesRecords = cmdbNodesRecords.associateBy { it.value1() }
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
                HostIdAndCloudAreaIdInfo(
                    nodeId = nodeIpToNodesRecords[svrIdToCmdbInfoMap[notInCCSvrIdList[index]]?.SvrIp]
                        ?.value2(),
                    bkCloudId = hostIdToCCinfo?.get(value)?.bkCloudId?.toLong(),
                    bkHostId = value
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
        val nodeIpList = cmdbNodesRecords.map { it.value3() }.toSet()
        // 节点：ip - cmdb record（从cmdb查到的，节点在cmdb中）
        val ipToCmdbInfoMap = tencentQueryFromCmdbService.queryCmdbInfoFromIp(nodeIpList)
        // 2.1 不在cmdb中，置空 host_id 和 云区域id, 对应节点的 NODE_STATUS字段 要改成 NOT_IN_CMDB
        val invalidIpList = nodeIpList.filterNot { ipToCmdbInfoMap?.containsKey(it) ?: false }
        nodeDao.updateNodeNotInCmdb(dslContext, invalidIpList)
    }
}