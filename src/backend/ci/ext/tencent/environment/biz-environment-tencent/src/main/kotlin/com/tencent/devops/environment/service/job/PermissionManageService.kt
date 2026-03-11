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
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.dao.job.JobDao
import com.tencent.devops.environment.pojo.dto.CmdbNodeDTO
import com.tencent.devops.environment.pojo.job.jobreq.Host
import com.tencent.devops.environment.service.cmdb.TencentCmdbService
import org.jooq.DSLContext
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
        val result = jobDao.isJobInsExist(dslContext, projectId, jobInstanceId)
        logger.debug("isJobInsBelongToProj|projectId=$projectId|jobInstanceId=$jobInstanceId|result=$result")
        return result
    }

    fun recordJobInsToProj(projectId: String, jobInstanceId: Long, createUser: String) {
        val insertedRowNum = jobDao.addJobProjRecord(dslContext, projectId, jobInstanceId, createUser)
        if (insertedRowNum != 1) {
            logger.warn("recordJobInsToProj failed|insertedRowNum={}", insertedRowNum)
        }
    }

    fun isUserHasAllPermission(userId: String, projectId: String, allHostList: List<Host>) {
        // 所有host对应的T_NODE表中的记录
        val cmdbNodes = getNodesFromHostList(projectId, allHostList).toSet()
        logger.info("checkPermissionNodeIps=" + cmdbNodes.joinToString { it.nodeIp })
        checkUserOrNodeCreatedUserPermission(userId, cmdbNodes)
    }

    private fun getNodesFromHostList(
        projectId: String,
        hostList: List<Host>
    ): List<CmdbNodeDTO> {
        val hostIdHostList = mutableListOf<Host>()
        val cloudIpHostList = mutableListOf<Host>()
        hostList.map {
            if (null != it.bkHostId) hostIdHostList.add(it)
            else cloudIpHostList.add(it)
        }
        val hostIdList = hostIdHostList.mapNotNull { it.bkHostId }
        val cmdbNodesByHostIds = cmdbNodeDao.listCmdbNodesByHostIds(projectId, hostIdList)

        val ipList = cloudIpHostList.mapNotNull { it.ip }
        val cmdbNodesByIps = cmdbNodeDao.listCmdbNodesByIps(projectId, ipList)
        val cmdbNodesByCloudIps = mutableListOf<CmdbNodeDTO>()
        // 根据云区域ID过滤
        val ipToHostMap = hostList.associateBy { it.ip }
        cmdbNodesByIps.forEach {
            val ip = it.nodeIp
            val cmdbNodeCloudAreaId = it.cloudAreaId
            val inputCloudAreaId = ipToHostMap[ip]?.bkCloudId
            val inputCloudAreaIdMatchDefault = (inputCloudAreaId == null && cmdbNodeCloudAreaId == 0L) ||
                (inputCloudAreaId == 0L && cmdbNodeCloudAreaId == null)
            if (inputCloudAreaId == cmdbNodeCloudAreaId || inputCloudAreaIdMatchDefault) {
                cmdbNodesByCloudIps.add(it)
            }
        }
        return cmdbNodesByHostIds + cmdbNodesByCloudIps
    }

    /*
     *  校验当前用户/节点导入人是否为机器的主备负责人
     *  先从CMDB中用nodeIp查询机器的主备负责人，再与当前用户/节点导入人进行对比判定
     */
    fun checkUserOrNodeCreatedUserPermission(
        userId: String,
        cmdbNodes: Set<CmdbNodeDTO>
    ) {
        val nodeIpSet = cmdbNodes.map { it.nodeIp }.toSet() // 所有host对应的serverId
        val nodeIpToNodeMap = cmdbNodes.associateBy { it.nodeIp }

        val ipToCmdbServerMap = tencentCmdbService.queryServerByIp(nodeIpSet)
        val notInCmdbIpList = mutableListOf<String>()
        val noPermissionIpList = mutableListOf<String>()
        nodeIpSet.forEach { nodeIp ->
            val cmdbNodeDTO = nodeIpToNodeMap[nodeIp]
            val cmdbServerDTO = ipToCmdbServerMap[nodeIp]
            if (null == cmdbServerDTO) {
                notInCmdbIpList.add(nodeIp)
                return@forEach
            }
            val createdUser = cmdbNodeDTO?.createdUser!!
            if (!cmdbServerDTO.hasOperatorOrBak(userId) && !cmdbServerDTO.hasOperatorOrBak(createdUser)) {
                noPermissionIpList.add(nodeIp)
            }
        }

        if (noPermissionIpList.isNotEmpty() || notInCmdbIpList.isNotEmpty()) {
            val noPermissionIpToNodeMap = noPermissionIpList.map {
                nodeIpToNodeMap[it]
            }.associateBy { it?.nodeIp }
            logger.warn(
                "noPermissionIpList={}|notInCmdbIpList={}",
                noPermissionIpList.joinToString(),
                notInCmdbIpList.joinToString()
            )
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_IP_ILLEGAL,
                params = arrayOf(
                    notInCmdbIpList.joinToString(","),
                    noPermissionIpList.joinToString(","),
                    userId,
                    noPermissionIpList.joinToString(", ") { ip ->
                        ip + "(" + noPermissionIpToNodeMap[ip]?.createdUser + ")"
                    }
                )
            )
        }
    }
}
