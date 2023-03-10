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

package com.tencent.devops.environment.permission

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.model.environment.tables.records.TEnvRecord
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class StreamEnvironmentPermissionServiceImp @Autowired constructor(
    val client: Client,
    val dslContext: DSLContext,
    val nodeDao: NodeDao,
    val envDao: EnvDao,
    val tokenCheckService: ClientTokenService
) : EnvironmentPermissionService {

    override fun listEnvByPermission(userId: String, projectId: String, permission: AuthPermission): Set<Long> {
        if (!checkPermission(userId, projectId)) {
            return emptySet()
        }
        return getAllEnvInstance(projectId).map { HashUtil.decodeIdToLong(it) }.toSet()
    }

    override fun listEnvByViewPermission(
        userId: String,
        projectId: String
    ): Set<Long> {
        return listEnvByPermission(userId, projectId, AuthPermission.USE)
    }

    override fun listEnvByPermissions(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        // 后续如果产品侧调整, view不按操作类校验,此处需要调整为对应的AuthPermission
        if (!checkPermission(userId, projectId)) {
            return emptyMap()
        }
        val resultMap = mutableMapOf<AuthPermission, List<String>>()
        val instances = getAllEnvInstance(projectId)
        permissions.forEach {
            resultMap[it] = instances
        }
        return resultMap
    }

    override fun getEnvListResult(canListEnv: List<TEnvRecord>, envRecordList: List<TEnvRecord>): List<TEnvRecord> {
        return envRecordList
    }

    override fun checkEnvPermission(
        userId: String,
        projectId: String,
        envId: Long,
        permission: AuthPermission
    ): Boolean {
        return checkPermission(userId, projectId)
    }

    override fun checkEnvPermission(userId: String, projectId: String, permission: AuthPermission): Boolean {
        return checkPermission(userId, projectId)
    }

    override fun createEnv(userId: String, projectId: String, envId: Long, envName: String) {
        return
    }

    override fun updateEnv(userId: String, projectId: String, envId: Long, envName: String) {
        return
    }

    override fun deleteEnv(projectId: String, envId: Long) {
        return
    }

    override fun listNodeByPermission(userId: String, projectId: String, permission: AuthPermission): Set<Long> {
        if (!checkPermission(userId, projectId)) {
            return emptySet()
        }
        return getAllNodeInstance(projectId).map { HashUtil.decodeIdToLong(it) }.toSet()
    }

    override fun listNodeByPermissions(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        // 后续如果产品侧调整, view不按操作类校验,此处需要调整为对应的AuthPermission
        if (!checkPermission(userId, projectId)) {
            return emptyMap()
        }
        val resultMap = mutableMapOf<AuthPermission, List<String>>()
        val instances = getAllEnvInstance(projectId)
        permissions.forEach {
            resultMap[it] = instances
        }
        return resultMap
    }

    override fun listNodeByRbacPermission(
        userId: String,
        projectId: String,
        nodeRecordList: List<TNodeRecord>,
        authPermission: AuthPermission
    ): List<TNodeRecord> {
        return nodeRecordList
    }

    override fun checkNodePermission(
        userId: String,
        projectId: String,
        nodeId: Long,
        permission: AuthPermission
    ): Boolean {
        if (permission == AuthPermission.VIEW)
            return true
        return checkPermission(userId, projectId)
    }

    override fun checkNodePermission(userId: String, projectId: String, permission: AuthPermission): Boolean {
        return checkPermission(userId, projectId)
    }

    override fun createNode(userId: String, projectId: String, nodeId: Long, nodeName: String) {
        return
    }

    override fun updateNode(userId: String, projectId: String, nodeId: Long, nodeName: String) {
        return
    }

    override fun deleteNode(projectId: String, nodeId: Long) {
        return
    }

    private fun checkPermission(userId: String, projectId: String): Boolean {
        logger.info("StreamEnvironmentPermissionServiceImp user:$userId projectId: $projectId ")
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId = userId,
            token = tokenCheckService.getSystemToken(null) ?: "",
            action = AuthPermission.ENABLE.value, // 环境,节点类的校验都需要按操作类的校验方式校验。即便是view类型也按操作类
            projectCode = projectId,
            resourceCode = ""
        ).data ?: false
    }

    // 拿到的数据统一为加密后的id
    private fun getAllNodeInstance(projectId: String): List<String> {
        val instanceIds = mutableListOf<String>()
        val repositoryInfos = nodeDao.listNodes(dslContext, projectId)
        repositoryInfos.map {
            instanceIds.add(HashUtil.encodeLongId(it.nodeId))
        }
        return instanceIds
    }

    // 拿到的数据统一为加密后的id
    private fun getAllEnvInstance(projectId: String): List<String> {
        val instanceIds = mutableListOf<String>()

        val repositoryInfos = envDao.list(dslContext, projectId)
        repositoryInfos.map {
            instanceIds.add(HashUtil.encodeLongId(it.envId))
        }
        return instanceIds
    }

    companion object {
        val logger = LoggerFactory.getLogger(StreamEnvironmentPermissionServiceImp::class.java)
    }
}
