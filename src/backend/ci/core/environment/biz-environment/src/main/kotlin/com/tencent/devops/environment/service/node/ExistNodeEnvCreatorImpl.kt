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

package com.tencent.devops.environment.service.node

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.enums.NodeSource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ExistNodeEnvCreatorImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val envDao: EnvDao,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val environmentPermissionService: EnvironmentPermissionService
) : EnvCreator {

    override fun id(): String {
        return NodeSource.EXISTING.name
    }

    override fun createEnv(projectId: String, userId: String, envCreateInfo: EnvCreateInfo): EnvironmentId {

        if (envCreateInfo.source.name != id()) {
            throw IllegalArgumentException("wrong nodeSourceType [${envCreateInfo.source}] in [${id()}]")
        }

        if (envCreateInfo.nodeHashIds == null || envCreateInfo.nodeHashIds!!.isEmpty()) {
            var envId = 0L
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                envId = envDao.create(
                    context, userId, projectId, envCreateInfo.name, envCreateInfo.desc,
                    envCreateInfo.envType.name, ObjectMapper().writeValueAsString(envCreateInfo.envVars)
                )
                environmentPermissionService.createEnv(userId, projectId, envId, envCreateInfo.name)
            }
            return EnvironmentId(HashUtil.encodeLongId(envId))
        }

        val nodeLongIds = envCreateInfo.nodeHashIds!!.map { HashUtil.decodeIdToLong(it) }

        // 检查 node 权限
        val canUseNodeIds =
            environmentPermissionService.listNodeByPermission(userId, projectId, AuthPermission.USE)
        val unauthorizedNodeIds = nodeLongIds.filterNot { canUseNodeIds.contains(it) }
        if (unauthorizedNodeIds.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NO_USE_PERMISSSION,
                params = arrayOf(unauthorizedNodeIds.joinToString(",") { HashUtil.encodeLongId(it) })
            )
        }

        // 检查 node 是否存在
        val existNodes = nodeDao.listByIds(dslContext, projectId, nodeLongIds)
        val existNodeIds = existNodes.map { it.nodeId }.toSet()
        val notExistNodeIds = nodeLongIds.filterNot { existNodeIds.contains(it) }
        if (notExistNodeIds.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                params = arrayOf(notExistNodeIds.joinToString(",") { HashUtil.encodeLongId(it) })
            )
        }

        var envId = 0L
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            envId = envDao.create(
                context, userId, projectId, envCreateInfo.name, envCreateInfo.desc,
                envCreateInfo.envType.name, ObjectMapper().writeValueAsString(envCreateInfo.envVars)
            )
            envNodeDao.batchStoreEnvNode(context, nodeLongIds, envId, projectId)
            environmentPermissionService.createEnv(userId, projectId, envId, envCreateInfo.name)
        }
        return EnvironmentId(HashUtil.encodeLongId(envId))
    }
}
