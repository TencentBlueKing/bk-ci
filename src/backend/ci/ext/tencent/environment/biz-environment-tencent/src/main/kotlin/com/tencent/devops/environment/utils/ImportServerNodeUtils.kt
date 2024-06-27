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
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_IMPORT_EXCEED
import com.tencent.devops.environment.dao.ProjectConfigDao
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.pojo.job.cmdbreq.NewCmdbCondition
import com.tencent.devops.environment.pojo.job.cmdbreq.NewCmdbConditionValue
import com.tencent.devops.environment.pojo.job.cmdbres.NewCmdbData
import com.tencent.devops.environment.service.CmdbNodeService
import com.tencent.devops.environment.service.job.TencentQueryFromCmdbService
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
        return if (ips.isEmpty()) {
            val key = "env_node_buffer_cmdb_${userId}_${offset}_${limit}_$bakOperator"
            val buffer = redisOperation.get(key)
            if (buffer != null) {
                jacksonObjectMapper().readValue(buffer)
            } else {
                val cmdbNodePage = esbAgentClient.getUserCmdbNodeNew(userId, bakOperator, ips, offset, limit)
                redisOperation.set(key, jacksonObjectMapper().writeValueAsString(cmdbNodePage), 60)
                cmdbNodePage
            }
        } else {
            esbAgentClient.getUserCmdbNodeNew(userId, bakOperator, ips, offset, limit)
        }
    }

    /**
     * 通过主备负责人，调用新CMDB接口，获取名下机器（此接口不支持根据ip/serverId查询机器）
     * @param bakOperator false-主负责人，true-备份负责人
     * @param cmdbColumn 返回的机器信息字段
     * @return 查询到的机器信息列表
     */
    fun getCmdbNodeByMaintainer(
        tencentQueryFromCmdbService: TencentQueryFromCmdbService,
        redisOperation: RedisOperation,
        userId: String,
        bakOperator: Boolean,
        page: Int,
        pageSize: Int,
        vararg cmdbColumn: String
    ): NewCmdbData? {
        val key = "env_node_buffer_cmdb_${userId}_${page}_${pageSize}_$bakOperator"
        val buffer = redisOperation.get(key)
        var currentNodePage: NewCmdbData? = NewCmdbData(list = listOf(), scrollId = null, hasNext = null)
        return if (!buffer.isNullOrEmpty()) {
            jacksonObjectMapper().readValue(buffer)
        } else {
            // 查询后，若缓存没有，则对用户名下机器根据当前分页做缓存(此处CMDB仅支持游标故向后缓存3页的游标，避免用户直接跳转某页时从头查CMDB)
            // 游标缓存在用户跳转到第一页或3分钟过期后清除
            var haveNext = true
            var currentScrollId = "0"
            var currentPage = 1
            if (!bakOperator) {
                while (haveNext) {
                    val currentKey = "env_node_buffer_cmdb_${userId}_${currentPage}_${pageSize}_false"
                    currentNodePage = tencentQueryFromCmdbService.queryNewCmdbInfoByBusiness(
                        newCmdbCondition = NewCmdbCondition(
                            maintainer = NewCmdbConditionValue(
                                operator = CmdbNodeService.CMDB_QUERY_OPERATION_IN,
                                value = userId.toList()
                            )
                        ),
                        size = pageSize,
                        scrollId = currentScrollId,
                        newReqColumn = cmdbColumn
                    )
                    redisOperation.set(currentKey, jacksonObjectMapper().writeValueAsString(currentNodePage?.scrollId), 360)
                    haveNext = currentNodePage?.hasNext ?: false
                    currentScrollId = currentNodePage?.scrollId ?: ""
                    if (currentPage == page) break
                    currentPage++
                }
                // 异步继续将剩下的3页缓存
                val remanentTask = object : Runnable {
                    var count = 0
                    override fun run() {
                        // TODO
                    }
                }
            } else {
                tencentQueryFromCmdbService.queryNewCmdbInfoByBusiness(
                    newCmdbCondition = NewCmdbCondition(
                        maintainerBak = NewCmdbConditionValue(
                            operator = CmdbNodeService.CMDB_QUERY_OPERATION_IN,
                            value = userId.toList()
                        )
                    ),
                    size = pageSize,
                    scrollId = currentScrollId,
                    newReqColumn = cmdbColumn
                )
            }
            currentNodePage
        }
    }

    fun checkImportCount(
        dslContext: DSLContext,
        projectConfigDao: ProjectConfigDao,
        cmdbNodeDao: CmdbNodeDao,
        projectId: String,
        userId: String,
        toAddNodeCount: Int
    ) {
        val projectConfig = projectConfigDao.get(dslContext, projectId, userId)
        val importQuata = projectConfig.importQuota
        val existImportNodeCount = cmdbNodeDao.countImportNode(dslContext, projectId)
        if (toAddNodeCount + existImportNodeCount > importQuata) {
            throw ErrorCodeException(
                errorCode = ERROR_NODE_IMPORT_EXCEED,
                params = arrayOf(importQuata.toString())
            )
        }
    }
}
