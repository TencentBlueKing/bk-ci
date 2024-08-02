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

package com.tencent.devops.dispatch.utils

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import kotlin.math.min

@Suppress("UNUSED")
class ThirdPartyAgentEnvLock(
    redisOperation: RedisOperation,
    projectId: String,
    envId: String
) : RedisLock(
    redisOperation = redisOperation,
    lockKey = "DISPATCH_REDIS_LOCK_ENV_${projectId}_$envId",
    expiredTimeInSeconds = 60L
) {

    fun tryLock(timeout: Long = 1000, interval: Long = 100): Boolean {
        val sleepTime = min(interval, timeout) // sleep时间不超过timeout
        val start = System.currentTimeMillis()
        var tryLock = tryLock()
        while (timeout > 0 && !tryLock && timeout > (System.currentTimeMillis() - start)) {
            Thread.sleep(sleepTime)
            tryLock = tryLock()
        }
        return tryLock
    }
}
