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

package com.tencent.devops.log.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.dao.LogTagDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Suppress("ReturnCount")
@Service
class LogTagService @Autowired constructor(
    private val dslContext: DSLContext,
    private val logTagDao: LogTagDao,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(LogTagService::class.java)
        private const val LOG_SUBTAG = "log:build:tag:subTags:"
        private const val LOG_SUBTAG_LOCK = "log:build:tag:subTags:distribute:lock:"
        fun getSubTagsRedisKey(buildId: String, tagName: String) = LOG_SUBTAG + genBuildIdAndTagKey(buildId, tagName)
        private fun genBuildIdAndTagKey(buildId: String, tag: String) = "$buildId:$tag"
    }

    fun saveSubTag(buildId: String, tagName: String, subTag: String) {
        val originSubTags = getSubTags(buildId, tagName)
        if (originSubTags?.contains(subTag) == true) return

        val subTags = mutableListOf(subTag)
        if (originSubTags != null) subTags.addAll(originSubTags)

        logTagDao.save(dslContext, buildId, tagName, JsonUtil.toJson(subTags))
        saveSubTagsToRedis(buildId, tagName, JsonUtil.toJson(subTags))

        logger.info("[$buildId|$tagName] Create new subTag in db and redis: $subTag")
    }

    fun getSubTags(buildId: String, tagName: String): List<String>? {
        val subTagsStr = redisOperation.get(getSubTagsRedisKey(buildId, tagName))

        if (subTagsStr == null) {
            val subTags = logTagDao.getSubTags(dslContext, buildId, tagName)
            if (subTags != null) {
                saveSubTagsToRedis(buildId, tagName, subTags)
                return JsonUtil.getObjectMapper().readValue(subTags, List::class.java) as List<String>
            }
            return subTags
        } else {
            return JsonUtil.getObjectMapper().readValue(subTagsStr, List::class.java) as List<String>
        }
    }

    private fun saveSubTagsToRedis(buildId: String, tagName: String, subTags: String) {
        val redisLock = RedisLock(redisOperation, LOG_SUBTAG_LOCK + genBuildIdAndTagKey(buildId, tagName), 10)
        try {
            redisLock.lock()
            redisOperation.set(getSubTagsRedisKey(buildId, tagName), subTags, TimeUnit.DAYS.toSeconds(1))
        } finally {
            redisLock.unlock()
        }
    }
}
