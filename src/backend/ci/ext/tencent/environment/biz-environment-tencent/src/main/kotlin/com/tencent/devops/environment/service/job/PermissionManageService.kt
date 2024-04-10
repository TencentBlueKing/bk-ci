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

import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.dao.job.JobDao
import com.tencent.devops.environment.pojo.job.jobreq.Host
import org.jooq.DSLContext
import org.jooq.Record5
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("PermissionManageService")
class PermissionManageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val jobDao: JobDao,
    private val cmdbNodeDao: CmdbNodeDao,
    private val tencentQueryFromCmdbService: TencentQueryFromCmdbService
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
        tencentQueryFromCmdbService.isOperatorOrBakOperator(userId, nodeRecords)
    }

    private fun getNodesFromHostList(
        dslContext: DSLContext,
        projectId: String,
        hostList: List<Host>
    ): List<Record5<Long, String, Long, Long, String>> {
        val recordByHostIdList = mutableListOf<Host>()
        val getRecordByIpAndBkCloudId = mutableListOf<Host>()
        hostList.map {
            if (null != it.bkHostId) recordByHostIdList.add(it)
            else getRecordByIpAndBkCloudId.add(it)
        }
        return cmdbNodeDao.getNodesFromHostListByBkHostId(dslContext, projectId, recordByHostIdList) +
            cmdbNodeDao.getNodesFromHostListByIpAndBkCloudId(dslContext, projectId, getRecordByIpAndBkCloudId)
    }
}