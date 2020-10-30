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

package com.tencent.devops.common.service.gray

import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class RepoGray {
    companion object {
        private val logger = LoggerFactory.getLogger(RepoGray::class.java)

        private const val REPO_GREY_KEY = "project:setting:repoGray"
        private const val REPO_NOT_GRAY_KEY = "project:setting:repoNotGray"
        private const val REPO_DEFAULT_GREY_KEY = "project:setting:repoGrayDefault"
    }

    private val cache = CacheBuilder.newBuilder()
        .maximumSize(10)
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build<String/*Redis Keys*/, Set<String>/*Project Names*/>()

    fun addGrayProject(projectId: String, redisOperation: RedisOperation) {
        redisOperation.addSetValue(REPO_GREY_KEY, projectId)
        try {
            cache.invalidate(REPO_GREY_KEY)
        } catch (ignored: Exception) {
        }
    }

    fun removeGrayProject(projectId: String, redisOperation: RedisOperation) {
        redisOperation.removeSetMember(REPO_GREY_KEY, projectId)
        try {
            cache.invalidate(REPO_GREY_KEY)
        } catch (ignored: Exception) {
        }
    }

    fun addNotGrayProject(projectId: String, redisOperation: RedisOperation) {
        redisOperation.addSetValue(REPO_NOT_GRAY_KEY, projectId)
        try {
            cache.invalidate(REPO_NOT_GRAY_KEY)
        } catch (ignored: Exception) {
        }
    }

    fun removeNotGrayProject(projectId: String, redisOperation: RedisOperation) {
        redisOperation.removeSetMember(REPO_NOT_GRAY_KEY, projectId)
        try {
            cache.invalidate(REPO_NOT_GRAY_KEY)
        } catch (ignored: Exception) {
        }
    }

    fun isGray(projectId: String, redisOperation: RedisOperation): Boolean {
        return when {
            getProjects(REPO_NOT_GRAY_KEY, redisOperation).contains(projectId) -> false
            getProjects(REPO_GREY_KEY, redisOperation).contains(projectId) -> true
            else -> defaultGray(redisOperation)
        }
    }

    fun grayProjectSet(redisOperation: RedisOperation) =
        (redisOperation.getSetMembers(REPO_GREY_KEY) ?: emptySet()).filter { !it.isBlank() }.toSet()

    private fun defaultGray(redisOperation: RedisOperation): Boolean {
        return redisOperation.get(REPO_DEFAULT_GREY_KEY) == "true"
    }

    private fun getProjects(redisKey: String, redisOperation: RedisOperation): Set<String> {
        var value = cache.getIfPresent(redisKey)
        if (value != null) {
            return value
        }
        synchronized(this) {
            value = cache.getIfPresent(redisKey)
            if (value != null) {
                return value!!
            }
            logger.info("refresh $redisKey from redis")
            value = redisOperation.getSetMembers(redisKey) ?: emptySet()
            cache.put(redisKey, value)
        }
        return value!!
    }
}
