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

package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.dao.ShortUrlDao
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import org.hashids.Hashids
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class ShortUrlService @Autowired constructor(
    private val dslContext: DSLContext,
    private val shortUrlDao: ShortUrlDao,
    private val redisOperation: RedisOperation
) {
    private val hashids = Hashids(HASH_SALT, 8)

    fun createShortUrl(url: String, ttl: Int): String {
        val shortUrlCache = redisOperation.get(url)
        return if (shortUrlCache != null) {
            logger.info("get short url from cache, url: $url, shortUrl: $shortUrlCache")
            shortUrlCache
        } else {
            val expireTime = LocalDateTime.now().plusSeconds(ttl.toLong())
            val urlId = shortUrlDao.create(dslContext, url, "devops", expireTime)
            val shortUrl = "${HomeHostUtil.shortUrlServerHost()}/${encodeLongId(urlId)}"
            logger.info("createShortUrl, url: $url, ttl: $ttl, shortUrl: $shortUrl")
            redisOperation.set(url, shortUrl, TimeUnit.MINUTES.toSeconds(SHORT_URL_CACHE_EXPIRED_MINUTE))
            shortUrl
        }
    }

    fun getRedirectUrl(urlId: String): String {
        val longId = decodeIdToLong(urlId)
        logger.info("visitShortUrl, urlId: $urlId($longId)")
        val shortUrl = shortUrlDao.getOrNull(dslContext, longId)
        if (shortUrl == null || shortUrl.expiredTime.isBefore(LocalDateTime.now())) {
            logger.info("short url expired, urlId: $urlId($longId)")
            return "${HomeHostUtil.shortUrlServerHost()}/sorry"
        }
        return shortUrl.url
    }

    fun encodeLongId(id: Long): String {
        return hashids.encode(id)
    }

    fun decodeIdToLong(hash: String): Long {
        val ids = hashids.decode(hash)
        return if (ids == null || ids.isEmpty()) {
            0L
        } else {
            ids[0]
        }
    }

    companion object {
        private const val HASH_SALT = "jHy^2(@So7"
        private val logger = LoggerFactory.getLogger(ShortUrlService::class.java)
        private const val SHORT_URL_CACHE_EXPIRED_MINUTE = 1L
    }
}
