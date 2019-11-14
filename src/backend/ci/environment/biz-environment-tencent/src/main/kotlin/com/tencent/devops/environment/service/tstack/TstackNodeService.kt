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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.environment.service.tstack

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.environment.client.TstackClient
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.tstack.TstackNodeDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.tstack.TstackNode
import org.apache.commons.lang.StringUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TstackNodeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val tstackNodeDao: TstackNodeDao,
    private val nodeDao: NodeDao,
    private val tstackClient: TstackClient,
    private val environmentPermissionService: EnvironmentPermissionService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TstackNodeService::class.java)
    }

    fun destroyTstackVm(tstackVmId: String) {
    }

    fun updateAvailable(hashId: String, available: Boolean) {
        val id = HashUtil.decodeIdToLong(hashId)
        tstackNodeDao.updateAvailable(dslContext, id, available)
    }

    fun listAvailableVm(projectId: String): List<TstackNode> {
        val vms = tstackNodeDao.listAvailableVm(dslContext, projectId)
        return vms.map {
            TstackNode(
                hashId = HashUtil.encodeLongId(it.id),
                nodeHashId = null,
                projectId = it.projectId,
                tstackVmId = it.tstackVmId,
                ip = it.vmIp,
                name = it.vmName,
                nodeType = NodeType.TSTACK.name,
                nodeStatus = NodeStatus.NORMAL.name,
                os = OS.WINDOWS.name,
                osVersion = it.vmOsVersion,
                cpu = it.vmCpu,
                memory = it.vmMemory,
                available = it.available,
                createdTime = it.createdTime.timestamp(),
                updatedTime = it.updatedTime.timestamp()
            )
        }
    }

    fun get(projectId: String, hashId: String): TstackNode? {
        val id = HashUtil.decodeIdToLong(hashId)
        val statckVmRecord = tstackNodeDao.getOrNull(dslContext, projectId, id) ?: return null
        return TstackNode(
            hashId = HashUtil.encodeLongId(statckVmRecord.id),
            nodeHashId = null,
            projectId = statckVmRecord.projectId,
            tstackVmId = statckVmRecord.tstackVmId,
            ip = statckVmRecord.vmIp,
            name = statckVmRecord.vmName,
            nodeType = NodeType.TSTACK.name,
            nodeStatus = NodeStatus.NORMAL.name,
            os = OS.WINDOWS.name,
            osVersion = statckVmRecord.vmOsVersion,
            cpu = statckVmRecord.vmCpu,
            memory = statckVmRecord.vmMemory,
            available = statckVmRecord.available,
            createdTime = statckVmRecord.createdTime.timestamp(),
            updatedTime = statckVmRecord.updatedTime.timestamp()
        )
    }

    fun getVncToken(projectId: String, nodeHashId: String): String {
        val nodeId = HashUtil.decodeIdToLong(nodeHashId)
        val statckVmRecord = tstackNodeDao.getByNodeId(dslContext = dslContext, projectId = projectId, nodeId = nodeId)
            ?: throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS)
        return tstackClient.createVncToken(statckVmRecord.tstackVmId)
    }

    fun assignTstackNode(projectId: String, stackNodeId: String, user: String): String {
        val longId = HashUtil.decodeIdToLong(stackNodeId)
        val tstackNode = tstackNodeDao.getOrNull(dslContext, longId)
            ?: throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS)
        if (StringUtils.isNotBlank(tstackNode.projectId) || tstackNode.nodeId != null) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_HAD_BEEN_ASSIGN)
        }

        var nodeHashId = ""
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val nodeId = nodeDao.addNode(
                context,
                projectId,
                tstackNode.vmIp,
                tstackNode.vmName,
                tstackNode.vmOsVersion,
                NodeStatus.NORMAL,
                NodeType.TSTACK,
                user
            )
            val nodeRecord = nodeDao.get(context, projectId, nodeId)!!
            tstackNodeDao.setNodeIdAndProjectId(context, longId, nodeId, projectId)
            environmentPermissionService.deleteNode(projectId, nodeRecord.nodeId)

            nodeHashId = HashUtil.encodeLongId(nodeId)
        }

        return nodeHashId
    }

    fun unassignTstackNode(projectId: String, stackNodeId: String): Boolean {
        val longId = HashUtil.decodeIdToLong(stackNodeId)
        val tstackNode = tstackNodeDao.getOrNull(dslContext, longId)
            ?: throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS)
        if (tstackNode.nodeId != null) {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val nodeRecord = nodeDao.get(context, projectId, tstackNode.nodeId)
                if (nodeRecord == null) {
                    logger.warn("tstack node not exists")
                } else {
                    nodeDao.batchDeleteNode(context, projectId, listOf(tstackNode.nodeId))
                    tstackNodeDao.cleanNodeIdAndProjectId(context, longId)
                    environmentPermissionService.deleteNode(projectId, nodeRecord.nodeId)
                }
            }
        }

        return true
    }
}
