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

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import org.slf4j.LoggerFactory

/**
 * 主要用于一些同一时间只允许有一个实例运行的地方。
 */
open class RedisCallLimit(
    private val redisOperation: RedisOperation,
    private val lockKey: String,
    private val expiredTimeInSeconds: Long
) : AutoCloseable {
    companion object {
        private val logger = LoggerFactory.getLogger(RedisCallLimit::class.java)
    }

    private val redisLock = RedisLock(redisOperation, lockKey, expiredTimeInSeconds)

    /**
     *
     *
     * @return 该 lock 需要放在 finally 外
     * @throws CustomException 已经存在 key ，说明是重复请求
     */
    fun lock(): RedisCallLimit {
        val result = redisLock.tryLock()
        if (!result) {
            logger.warn("$lockKey call duplicate, reject it.")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REPEAT_REQUEST.errorCode,
                defaultMessage = ErrorCodeEnum.REPEAT_REQUEST.formatErrorMessage
            )
        }
        return this
    }

    override fun close() {
        redisLock.close()
    }
}
