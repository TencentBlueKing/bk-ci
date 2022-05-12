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

package com.tencent.bkrepo.common.lock.service

import com.tencent.bkrepo.common.lock.dao.MongoDistributedLockDao

/**
 * 分布式锁服务实现类
 */
class MongoDistributedLock(
    private val lockDao: MongoDistributedLockDao
) {
    /**
     * mongodb实现的锁不支持重入
     */
    fun acquireLock(key: String, expireTime: Long): Boolean {
        if (key.isBlank()) return false
        val lockInfo = lockDao.findByKey(key)
        lockInfo?.let {
            if (it.expireTime >= System.currentTimeMillis()) {
                // key 已被人获取
                return false
            } else {
                // 删除过期key
                deleteExpireKey(key, System.currentTimeMillis())
            }
        }
        try {
            // 新生成key
            val newLockInfo = lockDao.incrByKeyWithExpire(key, VALUE, System.currentTimeMillis() + expireTime)
            newLockInfo?.let {
                if (newLockInfo.value == VALUE) {
                    return true
                } else if (newLockInfo.value > VALUE) {
                    return false
                }
            }
        } catch (e: Exception) {
            // 当出现duplicatekey时，说明key已被其他获取
            return false
        }
        return false
    }

    fun releaseLock(key: String) {
        if (key.isNotBlank()) {
            lockDao.deleteByKey(key)
        }
    }

    private fun deleteExpireKey(key: String, expireTime: Long) {
        if (key.isNotBlank()) {
            lockDao.deleteExpireKey(key, expireTime)
        }
    }

    companion object {
        private const val VALUE: Int = 1
    }
}
