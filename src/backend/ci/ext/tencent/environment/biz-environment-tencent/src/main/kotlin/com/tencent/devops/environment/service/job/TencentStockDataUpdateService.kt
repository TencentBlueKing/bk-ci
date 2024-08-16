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
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.constant.T_NODE_SERVER_ID
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.pojo.job.jobresp.CCUpdateInfo
import com.tencent.devops.environment.service.CmdbNodeService
import com.tencent.devops.environment.service.RedisLockService
import com.tencent.devops.environment.service.cc.TencentQueryFromCCService
import com.tencent.devops.environment.service.cmdb.TencentCmdbService
import com.tencent.devops.environment.utils.ComputeTimeUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 存量数据补全服务，用于手动触发填充因新增字段带来的不完整存量数据
 */
@Service("TencentStockDataUpdateService")
@Primary
class TencentStockDataUpdateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val cmdbNodeDao: CmdbNodeDao,
    private val tencentCmdbService: TencentCmdbService,
    private val tencentQueryFromCCService: TencentQueryFromCCService,
    private val cmdbNodeService: CmdbNodeService,
    private val redisLockService: RedisLockService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TencentStockDataUpdateService::class.java)

        private const val ADD_NODES_TO_CC_TIMEOUT_LOCK_KEY = "add_nodes_to_cc_timeout_lock"
        private const val WRITE_SERVER_ID_TIMEOUT_LOCK_KEY = "write_server_id_timeout_lock"

        private const val DEFAULT_PAGE_SIZE = 100
    }

    /**
     * addNodeToCC:
     * 分组执行，每次遍历100条记录。
     * 将不在CC中的 类型为CMDB 的节点，添加到CC中，并返回host_id、云区域id和操作系统类型，将host_id、云区域id和操作系统类型写入表中
     * 存量数据更新任务：执行一次。
     * 提供apigw接口
     */
    fun addNodesToCCOnce() {
        redisLockService.taskWithRedisLock(ADD_NODES_TO_CC_TIMEOUT_LOCK_KEY, ::addNodesToCC)
    }

    /**
     * writeServerId：
     * 分组执行，每次遍历100条记录。
     * 通过ip查询节点对应的server_id，写入数据库。
     * 执行一次，提供apigw接口
     */
    fun writeServerIdOnce() {
        redisLockService.taskWithRedisLock(WRITE_SERVER_ID_TIMEOUT_LOCK_KEY, ::writeServerId)
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
}
