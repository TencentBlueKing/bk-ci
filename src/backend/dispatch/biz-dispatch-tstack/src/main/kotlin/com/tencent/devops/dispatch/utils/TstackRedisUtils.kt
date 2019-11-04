/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
package com.tencent.devops.dispatch.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.pojo.TstackContainerInfo
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TstackRedisUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {

    fun getTstackContainerInfo(key: String): TstackContainerInfo? {
        val containerData = redisOperation.get(key) ?: return null
        try {
            return objectMapper.readValue(containerData, TstackContainerInfo::class.java)
        } catch (t: Throwable) {
            logger.warn("Fail to covert the tstack container data to object($containerData)", t)
        }
        return null
    }

    fun setTstackContainerInfo(key: String, containerInfo: TstackContainerInfo) {
        redisOperation.set(key, objectMapper.writeValueAsString(containerInfo))
    }

    fun getTstackRedisBuild(ip: String): RedisBuild? {
        val build = redisOperation.get(ip) ?: return null
        try {
            return objectMapper.readValue(build, RedisBuild::class.java)
        } catch (t: Throwable) {
            logger.warn("Fail to covert the redis build to object($build)", t)
        }
        return null
    }

    fun setTstackRedisBuild(ip: String, redisBuild: RedisBuild) {
        redisOperation.set(ip, objectMapper.writeValueAsString(redisBuild))
    }

    fun deleteTstackRedisBuild(ip: String) {
        redisOperation.delete(ip)
    }

    fun deleteTstackContainerInfo(key: String) {
        redisOperation.delete(key)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TstackRedisUtils::class.java)
    }
}
