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

package com.tencent.devops.process.engine.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineCacheManager @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCacheManager::class.java)
        const val buildModelKey = "process:pipeline:build:model:"
    }

    fun getBuildModel(buildId: String): Model? {

        val string = redisOperation.get(buildModelKey + buildId)
        return try {
            objectMapper.readValue(string, Model::class.java)
        } catch (e: Exception) {
            logger.warn("[$buildId]| getBuildModel failed $e", e)
            null
        }
    }

    fun setBuildModel(buildId: String, model: Model, timeOutSeconds: Long = 300) {
        setBuildModel(buildId, objectMapper.writeValueAsString(model))
    }

    fun setBuildModel(buildId: String, modelJsonString: String, timeOutSeconds: Long = 300) {
        redisOperation.set(buildModelKey + buildId, modelJsonString, timeOutSeconds)
    }
}
