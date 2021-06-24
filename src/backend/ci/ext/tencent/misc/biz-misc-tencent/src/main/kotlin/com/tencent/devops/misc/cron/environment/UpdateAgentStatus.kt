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

package com.tencent.devops.misc.cron.environment

import com.tencent.devops.common.environment.agent.client.EsbAgentClient
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.DEFAULT_SYTEM_USER
import com.tencent.devops.misc.dao.environment.EnvironmentNodeDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Suppress("ALL", "UNUSED")
class UpdateAgentStatus @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: EnvironmentNodeDao,
    private val redisOperation: RedisOperation,
    private val esbAgentClient: EsbAgentClient
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UpdateAgentStatus::class.java)
        private const val LOCK_KEY = "env_cron_updateAgentStatus"
        private const val EXPIRED_IN_SECOND = 600L
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 15000)
    fun runUpdateAgentStatus() {
        logger.info("runUpdateAgentStatus")
        val lock = RedisLock(redisOperation, lockKey = LOCK_KEY, expiredTimeInSeconds = EXPIRED_IN_SECOND)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip")
                return
            }

            val allServerNodes = nodeDao.listAllServerNodes(dslContext)
            if (allServerNodes.isEmpty()) {
                return
            }

            val allIps = allServerNodes.map { it.nodeIp }.toSet()
            val agentStatusMap = esbAgentClient.getAgentStatus(DEFAULT_SYTEM_USER, allIps)

            allServerNodes.forEach {
                it.agentStatus = agentStatusMap[it.nodeIp] ?: false
            }

            nodeDao.batchUpdateNode(dslContext, allServerNodes)
        } catch (ignore: Throwable) {
            logger.warn("update agent status failed", ignore)
        } finally {
            lock.unlock()
        }
    }
}
