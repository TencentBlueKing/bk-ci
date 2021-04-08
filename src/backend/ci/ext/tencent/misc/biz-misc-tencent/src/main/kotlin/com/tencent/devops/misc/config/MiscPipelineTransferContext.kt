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

package com.tencent.devops.misc.config

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.locks.ReentrantLock

@Component
class MiscPipelineTransferContext @Autowired constructor(private val redisOperation: RedisOperation) {

    companion object {
        private const val LOCK_KEY = "misc:pipeline:transfer:lock"
        private const val TRANSFER_PROJECT_CHANNELS_KEY = "misc:pipeline:transfer:channels"
        private const val TRANSFER_PROJECT_LIST_KEY = "misc:pipeline:transfer:project:list"
        private const val TRANSFER_PROJECT_ID_KEY = "misc:pipeline:transfer:project:id"
        private const val TRANSFER_PROJECT_BATCH_SIZE_KEY = "misc:pipeline:transfer:project:batchSize"
        private const val expiredTimeInSeconds: Long = 3000
        private val channelSet = mutableSetOf<String>()
        private val reentrantLock = ReentrantLock()
    }

    private val lock = RedisLock(redisOperation, LOCK_KEY, expiredTimeInSeconds)

    fun switch(): Boolean = redisOperation.get("misc:pipeline:transfer:switch") == "1"

    fun tryLock(): Boolean = lock.tryLock()

    fun unLock() = lock.unlock()

    /**
     * return p1,p2,p3
     */
    fun needTransferProjectIdList(): List<String> {
        val projectIdListConfig = redisOperation.get(TRANSFER_PROJECT_LIST_KEY)
        if (!projectIdListConfig.isNullOrBlank()) {
            return projectIdListConfig.split(",")
        }
        return emptyList()
    }

    /**
     * return Long
     */
    fun getLastTransferProjectSeqId() = redisOperation.get(TRANSFER_PROJECT_ID_KEY)?.toLong()

    fun clearLastTransferProjectId() = redisOperation.delete(TRANSFER_PROJECT_ID_KEY)

    fun dealProjectBatchSize(): Int = redisOperation.get(TRANSFER_PROJECT_BATCH_SIZE_KEY)?.toInt() ?: 500

    fun setLastProjectSeqId(maxHandleProjectPrimaryId: Long) {
        redisOperation.set(TRANSFER_PROJECT_ID_KEY, maxHandleProjectPrimaryId.toString())
    }

    fun checkTransferChannel(channel: String): Boolean {
        if (channelSet.isEmpty()) {
            try {
                reentrantLock.lock()
                val channels = redisOperation.get(TRANSFER_PROJECT_CHANNELS_KEY) ?: "CODECC"
                channelSet.addAll(channels.split(","))
            } finally {
                if (reentrantLock.isLocked) {
                    reentrantLock.unlock()
                }
            }
        }
        return channelSet.contains(channel)
    }
}
