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

package com.tencent.bkrepo.common.redis.concurrent

import com.tencent.bkrepo.common.redis.RedisOperation

class SimpleRateLimiter(private val redisOperation: RedisOperation) {

    /**
     * 在[seconds]秒内，获取锁[lockKey]数量不超过[bucketSize]，否则返回false
     */
    fun acquire(bucketSize: Int, lockKey: String, seconds: Long = 60): Boolean {
        return if (redisOperation.increment(lockKey, 1) ?: 1 <= bucketSize) {
            redisOperation.expire(lockKey, seconds)
            true
        } else {
            release(lockKey)
            redisOperation.expire(lockKey, seconds)
            false
        }
    }

    /**
     * 释放获得的锁[lockKey]
     */
    fun release(lockKey: String) {
        if (redisOperation.hasKey(lockKey)) {
            redisOperation.increment(lockKey, -1)
        }
    }
}
