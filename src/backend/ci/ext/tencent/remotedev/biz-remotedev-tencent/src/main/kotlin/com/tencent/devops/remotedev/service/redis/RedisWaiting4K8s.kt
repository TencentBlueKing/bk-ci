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

package com.tencent.devops.remotedev.service.redis

import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory

/**
 * 用于等待dispatch k8s返回消息
 */
open class RedisWaiting4K8s(
    private val redisOperation: RedisOperation,
    private val lockKey: String,
    private val expiredCount: Int = 90
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RedisWaiting4K8s::class.java)
    }

    fun waiting(): Boolean {
        var expired = expiredCount
        while (expired-- > 0) {
            val result = redisOperation.get(lockKey)
            if (result != null) {
                redisOperation.delete(lockKey)
                logger.info("RedisWaiting4K8s get $lockKey for $result")
                return result == "true"
            }
            Thread.sleep(1000)
        }
        logger.info("RedisWaiting4K8s get $lockKey time out")
        return false
    }
}
