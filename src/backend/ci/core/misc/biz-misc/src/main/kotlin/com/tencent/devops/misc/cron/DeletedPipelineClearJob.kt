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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.misc.cron

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.api.service.callback.ServicePipelineCallbackResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DeletedPipelineClearJob @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(DeletedPipelineClearJob::class.java)
        private const val KEY_LOCK = "misc:DeletedPipelineClearJob:lock"
        private const val KEY_LAST_TRIGGER_TIME = "misc:DeletedPipelineClearJob:lastTriggerTime"
    }

    //每月15日00:00开始清理已删除流水线
    @Scheduled(cron = "0 0 0 15 * ?")
    fun clearDeletedPipelines() {
        logger.info("clearDeletedPipelines")
        val lock = RedisLock(redisOperation, KEY_LOCK, 60)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip")
                return
            }
            val lastTriggerTime = redisOperation.get(KEY_LAST_TRIGGER_TIME)
            val nowTime = System.currentTimeMillis()
            if (lastTriggerTime != null && nowTime - lastTriggerTime.toLong() < 5 * 60 * 1000) {
                logger.info("clearDeletedPipelines fires too frequently")
                return
            }
            //回调清理接口
            val result = client.get(ServicePipelineCallbackResource::class).clear()
            logger.info("callback result=$result")
            redisOperation.set(KEY_LAST_TRIGGER_TIME, System.currentTimeMillis().toString(), expired = false)
        } catch (t: Throwable) {
            logger.warn("clearDeletedPipelines failed", t)
        } finally {
            lock.unlock()
        }
    }
}
