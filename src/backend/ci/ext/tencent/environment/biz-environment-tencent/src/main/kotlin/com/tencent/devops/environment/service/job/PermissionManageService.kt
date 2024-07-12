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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.constant.T_NODE_CREATED_USER
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.constant.T_NODE_SERVER_ID
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.dao.job.JobDao
import com.tencent.devops.environment.pojo.job.jobreq.Host
import com.tencent.devops.environment.service.cmdb.TencentCmdbService
import org.jooq.DSLContext
import org.jooq.Record6
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("PermissionManageService")
class PermissionManageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val jobDao: JobDao,
    private val cmdbNodeDao: CmdbNodeDao,
    private val tencentCmdbService: TencentCmdbService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PermissionManageService::class.java)
    }

    fun isJobInsBelongToProj(projectId: String, jobInstanceId: Long): Boolean {
        val jobProjRecordExist = jobDao.isJobInsExist(dslContext, projectId, jobInstanceId)
        logger.info("[isJobInsBelongToProj] $projectId: $jobProjRecordExist")
        return jobProjRecordExist
    }

    fun recordJobInsToProj(projectId: String, jobInstanceId: Long, createUser: String) {
        val jobProjInsertResult = jobDao.addJobProjRecord(dslContext, projectId, jobInstanceId, createUser)
        logger.info("[recordJobInsToProj] $jobProjInsertResult row(s) of data have been inserted into the table.")
    }

    fun isUserHasAllPermission(userId: String, projectId: String, allHostList: List<Host>) {
        val nodeRecords = getNodesFromHostList(dslContext, projectId, allHostList).toSet() // 所有host对应的T_NODE表中的记录
        logger.info(
            "Wait to detect permission node ip(s): " +
                nodeRecords.mapNotNull { it[T_NODE_NODE_IP] as? String }.joinToString()
        )
        // 判断：用户or节点导入人 是机器的主备负责人（用户：函数中形参userId；节点导入人：T_NODE表中的createdUser）
        isOperatorOrBakOperator(userId, nodeRecords)
    }

    private fun getNodesFromHostList(
        dslContext: DSLContext,
        projectId: String,
        hostList: List<Host>
    ): List<Record6<Long, String, Long, Long, String, Long>> {
        val recordByHostIdList = mutableListOf<Host>()
        val getRecordByIpAndBkCloudId = mutableListOf<Host>()
        hostList.map {
            if (null != it.bkHostId) recordByHostIdList.add(it)
            else getRecordByIpAndBkCloudId.add(it)
        }
        return cmdbNodeDao.getNodesFromHostListByBkHostId(dslContext, projectId, recordByHostIdList) +
            cmdbNodeDao.getNodesFromHostListByIpAndBkCloudId(dslContext, projectId, getRecordByIpAndBkCloudId)
    }

    /*
     *  判断：用户or节点导入人 是机器的主备负责人（用户：函数中形参userId；节点导入人：T_NODE表中的createdUser）
     *  ext中实现：从cmdb中 用serverId查询机器的主备负责人（提示用户时，还是用ip标识机器）
     */
    fun isOperatorOrBakOperator(userId: String, nodeRecords: Set<Record6<Long, String, Long, Long, String, Long>>) {
        val nodeServerIdSet = nodeRecords.mapNotNull { it[T_NODE_SERVER_ID] as? Long }.toSet() // 所有host对应的serverId
        // 所有host的：serverId - 记录 映射
        val nodeServerIdToNodeMap = nodeRecords.associateBy {
            it[T_NODE_SERVER_ID] as? Long
        }

        val cmdbServerIdToCmdbDataMap = tencentCmdbService.queryServerByServerId(nodeServerIdSet)
        // 没有serverId的节点记录，也认为该节点不在CMDB中
        val ipNotInCmdb = nodeRecords.filterNot { nodeServerIdSet.contains(it[T_NODE_SERVER_ID] as? Long) }.map {
            it[T_NODE_NODE_IP] as String
        }.toMutableList()
        val noPermissionServerIdList = nodeServerIdSet.filter {
            if (null != cmdbServerIdToCmdbDataMap[it]) {
                val createdUser = nodeServerIdToNodeMap[it]?.get(T_NODE_CREATED_USER) as? String
                val operator = cmdbServerIdToCmdbDataMap[it]?.operator
                val isOperator = userId == operator || createdUser == operator
                val bakOperatorSet = cmdbServerIdToCmdbDataMap[it]?.bakOperatorList?.toSet() ?: emptySet()
                val isBakOperator = bakOperatorSet.contains(userId) || bakOperatorSet.contains(createdUser)
                !isOperator && !isBakOperator
            } else { // 机器不在CMDB中
                ipNotInCmdb.add(nodeServerIdToNodeMap[it]?.get(T_NODE_NODE_IP) as String)
                false
            }
        }
        if (noPermissionServerIdList.isNotEmpty() || ipNotInCmdb.isNotEmpty()) {
            val noPermissionIpToNodeMap = noPermissionServerIdList.map {
                nodeServerIdToNodeMap[it]
            }.associateBy { it?.get(T_NODE_NODE_IP) as String }
            val noPermissionIpList = noPermissionIpToNodeMap.keys
            logger.warn(
                "[isOperatorOrBakOperator] noPermissionIpList: ${noPermissionIpList.joinToString()}, " +
                    "notInCmdbIpList: ${ipNotInCmdb.joinToString()}"
            )
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_IP_ILLEGAL,
                params = arrayOf(
                    ipNotInCmdb.joinToString(","),
                    noPermissionIpList.joinToString(","),
                    userId,
                    noPermissionIpList.joinToString(", ") {
                        it + " - " + noPermissionIpToNodeMap[it]?.get(T_NODE_CREATED_USER) as? String
                    }
                )
            )
        }
    }
}
