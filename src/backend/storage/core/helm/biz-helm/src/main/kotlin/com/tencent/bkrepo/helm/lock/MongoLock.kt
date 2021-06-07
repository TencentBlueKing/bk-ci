/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.helm.lock

import com.tencent.bkrepo.helm.dao.MongoLockDao
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MongoLock(
    private val mongoLockDao: MongoLockDao
) {

    fun tryLock(lockKey: String, lockValue: String): Boolean {
        // 如果存在且value一致，则返回true
        val mongoValue = mongoLockDao.getByKey(lockKey, lockValue)
        if (mongoValue != null && mongoValue.requestId == lockValue) {
            return true
        }
        // 不存在则添加 且设置过期时间（单位ms）
        logger.info("Start to lock [$lockKey] of value[$lockValue] for $expiredTimeInSeconds sec")
        val result = mongoLockDao.incrByWithExpire(lockKey, lockValue, expiredTimeInSeconds)
        logger.info("Get the lock [$lockKey] result($result)")
        return result
    }

    /**
     * 释放锁
     * @param lockKey
     * @param lockValue
     */
    fun releaseLock(lockKey: String, lockValue: String): Boolean {
        val mongoValue = mongoLockDao.getByKey(lockKey, lockValue)
        var result = false
        if (mongoValue == null) {
            logger.info("the lock key ($lockKey) already unlock")
            return true
        }
        if (mongoValue.requestId == lockValue) {
            logger.info("It's owen unlock")
            result = true
        }
        if (mongoValue.requestId != lockValue) {
            logger.info("the lock ($lockKey) is not allowed to unlock")
            return false
        }
        if (result) {
            result = mongoLockDao.releaseLock(lockKey, lockValue)
            logger.info("release lock ($lockKey) success!")
        }
        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MongoLock::class.java)
        const val expiredTimeInSeconds = 30L
    }
}
