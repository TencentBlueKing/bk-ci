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

package com.tencent.devops.environment.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.environment.agent.client.EsbAgentClient
import com.tencent.devops.common.environment.agent.pojo.agent.CmdbServerPage
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.ProjectConfigDao
import org.jooq.DSLContext

@Suppress("ALL")
object ImportServerNodeUtils {

    fun getUserCmdbNodeNew(
        esbAgentClient: EsbAgentClient,
        redisOperation: RedisOperation,
        userId: String,
        bakOperator: Boolean,
        ips: List<String>,
        offset: Int,
        limit: Int
    ): CmdbServerPage {
        // 对没有IP条件的查询，做缓存
        if (ips.isEmpty()) {
            val key = "env_node_buffer_cmdb_${userId}_${offset}_${limit}_$bakOperator"
            val buffer = redisOperation.get(key)
            return if (buffer != null) {
                jacksonObjectMapper().readValue(buffer)
            } else {
                val cmdbNodePage = esbAgentClient.getUserCmdbNodeNew(userId, bakOperator, ips, offset, limit)
                redisOperation.set(key, jacksonObjectMapper().writeValueAsString(cmdbNodePage), 60)
                cmdbNodePage
            }
        }

        return esbAgentClient.getUserCmdbNodeNew(userId, bakOperator, ips, offset, limit)
    }

    fun checkImportCount(
        dslContext: DSLContext,
        projectConfigDao: ProjectConfigDao,
        nodeDao: NodeDao,
        projectId: String,
        userId: String,
        toAddNodeCount: Int
    ) {
        val projectConfig = projectConfigDao.get(dslContext, projectId, userId)
        val importQuata = projectConfig.importQuota
        val existImportNodeCount = nodeDao.countImportNode(dslContext, projectId)
        if (toAddNodeCount + existImportNodeCount > importQuata) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_IMPORT_EXCEED,
                params = arrayOf(importQuata.toString())
            )
        }
    }
}
