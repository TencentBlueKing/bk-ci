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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.dao.PipelineBuildContainerDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineBuildLimitService @Autowired constructor(
    val redisOperation: RedisOperation,
    val dslContext: DSLContext,
    val pipelineBuildContainerDao: PipelineBuildContainerDao
) {

    /**
     *  判断是否当前运行的job是否大于平台的配置
     */
    fun moreEngineMaxCount(buildId: String, containerId: String): Int {
        // 若已进入过的直接放行
        if (isAddRecord(buildId, containerId)) {
            return safeCount
        }

        // 若未配置,直接返回false
        val engineMaxRunningCount = redisOperation.get(executeMaxCountKey)
        if (engineMaxRunningCount.isNullOrEmpty()) {
            logger.info("redis config PROCESS_ENGINE_MAX_COUNT is empty")
            return safeCount
        }

        // 获取当前运行的job数据量
        val maxRunningCount = engineMaxRunningCount!!.toInt()
        val engineRunningCount = redisOperation.get(executeJobKey)
        if (engineRunningCount.isNullOrEmpty()) {
            logger.info("redis config PROCESS_ENGINE_RUNNING_JOB_COUNT is empty")
            return safeCount
        }

        // 当前运行数据需小于最大的配额
        val runningCount = engineRunningCount!!.toInt()
        if (runningCount < maxRunningCount) {
            return safeCount
        }
        logger.warn("runningJob more maxCount, maxCount: $maxRunningCount")
        return maxRunningCount
    }

    /**
     * 当前job运行数据+1
     */
    fun executeCountAdd() {
        redisOperation.increment(executeJobKey, 1)
    }

    /**
     * 当前job运行数据+1
     */
    fun executeCountLess() {
        redisOperation.increment(executeJobKey, -1)
    }

    /**
     * job运行数量减1
     */
    fun jobRunningCountLess(buildId: String, containerId: String) {
        // 只有已经加1过的build+container才可以进行减1
        if (isAddRecord(buildId, containerId)) {
            executeCountLess()
            // 清理redisKey
            deleteRecordRedis(buildId, containerId)
        }
    }

    /**
     * 设置已操作标签 buildId+containerId
     */
    fun setRecordToRedis(buildId: String, containerId: String) {
        redisOperation.set(getRecordKey(buildId, containerId), "true", Timeout.CONTAINER_MAX_MILLS.toLong(), true)
    }

    /**
     * 删除已操作标签 buildId+containerId
     */
    fun deleteRecordRedis(buildId: String, containerId: String) {
        redisOperation.delete(getRecordKey(buildId, containerId))
    }

    /**
     * 判断是否已经进行加1操作 buildId+containerId
     */
    fun isAddRecord(buildId: String, containerId: String): Boolean {
        val addFlag = redisOperation.get(getRecordKey(buildId, containerId))
        if (!addFlag.isNullOrEmpty()) {
            return true
        }
        return false
    }

    fun getSystemMaxCount(): Int {
        return redisOperation.get(executeMaxCountKey)?.toInt() ?: 0
    }

    private fun getRecordKey(buildId: String, containerId: String): String {
        return "$executeBuildRecordKey:$buildId:$containerId"
    }

    companion object {
        val logger = LoggerFactory.getLogger(PipelineBuildLimitService::class.java)
        const val executeJobKey = "PROCESS_ENGINE_RUNNING_JOB_COUNT"
        const val executeMaxCountKey = "PROCESS_ENGINE_MAX_COUNT"
        const val executeBuildRecordKey = "PIPELINE_EXECUTE_COUNT_RECORD_"
        const val safeCount = -1
    }
}
