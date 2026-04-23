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

package com.tencent.devops.ai.service

import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * AI 项目服务。
 *
 * 管理 AI 服务支持的项目列表，项目列表存储在 Redis 中。
 */
@Service
class AiProjectService @Autowired constructor(
    private val redisOperation: RedisOperation
) {

    fun getProjectList(userId: String): List<String> {
        logger.info("[AiProject] Getting project list for user: {}", userId)

        val projectIds = redisOperation.getSetMembers(PROJECT_LIST_KEY)
            ?.toList()
            ?: emptyList()

        logger.info(
            "[AiProject] Retrieved project list for user {}: total={}",
            userId, projectIds.size
        )

        return projectIds
    }

    fun getProjectListForOp(): List<String> {
        logger.info("[AiProject] Getting project list for op management")

        val projectIds = redisOperation.getSetMembers(PROJECT_LIST_KEY)
            ?.toList()
            ?: emptyList()

        logger.info("[AiProject] Retrieved project list for op: total={}", projectIds.size)

        return projectIds
    }

    fun updateProjectList(projectIds: List<String>) {
        logger.info("[AiProject] Updating project list: {}", projectIds)

        redisOperation.delete(PROJECT_LIST_KEY)
        if (projectIds.isNotEmpty()) {
            redisOperation.sadd(PROJECT_LIST_KEY, *projectIds.toTypedArray())
        }

        logger.info("[AiProject] Project list updated: total={}", projectIds.size)
    }

    fun addProject(projectId: String) {
        logger.info("[AiProject] Adding project: {}", projectId)
        redisOperation.sadd(PROJECT_LIST_KEY, projectId)
    }

    fun removeProject(projectId: String) {
        logger.info("[AiProject] Removing project: {}", projectId)
        redisOperation.removeSetMember(PROJECT_LIST_KEY, projectId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            AiProjectService::class.java
        )
        const val PROJECT_LIST_KEY = "ai:projects:list"
    }
}
