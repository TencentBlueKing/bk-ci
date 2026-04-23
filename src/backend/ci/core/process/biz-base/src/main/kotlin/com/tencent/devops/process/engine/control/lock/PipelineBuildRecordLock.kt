/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.engine.control.lock

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

/**
 * 构建记录锁抽象基类
 * 统一管理公共字段和行为，减少重复代码
 */
abstract class AbstractBuildRecordLock(
    redisOperation: RedisOperation,
    buildId: String,
    executeCount: Int,
    lockKeySuffix: String = ""
) : RedisLock(
    redisOperation = redisOperation,
    lockKey = buildLockKey(buildId, executeCount, lockKeySuffix),
    expiredTimeInSeconds = 10L
) {

    override fun decorateKey(key: String): String {
        // buildId，key无需加上集群信息前缀来区分
        return key
    }

    companion object {
        private const val LOCK_KEY_PREFIX = "process.build.record.lock"

        /**
         * 构建锁的key
         * @param buildId 构建ID
         * @param executeCount 执行次数
         * @param suffix 额外的后缀（如stageId、containerId、taskId）
         */
        private fun buildLockKey(buildId: String, executeCount: Int, suffix: String): String {
            return if (suffix.isBlank()) {
                "$LOCK_KEY_PREFIX.$buildId.$executeCount"
            } else {
                "$LOCK_KEY_PREFIX.$buildId.$suffix.$executeCount"
            }
        }
    }
}

/**
 * Pipeline级别的构建记录锁 - 暂时无用BuildIdLock功能冲突
 */
class PipelineBuildRecordLock(
    redisOperation: RedisOperation,
    buildId: String,
    executeCount: Int
) : AbstractBuildRecordLock(
    redisOperation = redisOperation,
    buildId = buildId,
    executeCount = executeCount,
    lockKeySuffix = ""
)

/**
 * Stage级别的构建记录锁
 */
class StageBuildRecordLock(
    redisOperation: RedisOperation,
    buildId: String,
    stageId: String,
    executeCount: Int
) : AbstractBuildRecordLock(
    redisOperation = redisOperation,
    buildId = buildId,
    executeCount = executeCount,
    lockKeySuffix = stageId
)

/**
 * Container级别的构建记录锁
 */
class ContainerBuildRecordLock(
    redisOperation: RedisOperation,
    buildId: String,
    containerId: String,
    executeCount: Int
) : AbstractBuildRecordLock(
    redisOperation = redisOperation,
    buildId = buildId,
    executeCount = executeCount,
    lockKeySuffix = containerId
)

/**
 * Task级别的构建记录锁
 */
class TaskBuildRecordLock(
    redisOperation: RedisOperation,
    buildId: String,
    taskId: String,
    executeCount: Int
) : AbstractBuildRecordLock(
    redisOperation = redisOperation,
    buildId = buildId,
    executeCount = executeCount,
    lockKeySuffix = taskId
)
