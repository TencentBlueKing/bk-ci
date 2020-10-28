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
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class Gray {

    @Value("\${project.gray.v2:#{null}}")
    private val grayFlag: String? = "false"

    var gray: Boolean? = null

    private val redisKey = "project:setting:gray:v2" // v2灰度项目列表存在redis的标识key

    private val codeCCRedisKey = "project:setting:gray:codecc:v2" // v2 CodeCC灰度项目列表存在redis的标识key

    private val cache = CacheBuilder.newBuilder()
        .maximumSize(10)
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build<String/*Redis Keys*/, Set<String>/*Project Names*/>()

    fun isGray(): Boolean {
        if (gray == null) {
            synchronized(this) {
                if (gray == null) {
                    gray = !grayFlag.isNullOrBlank() && grayFlag!!.toBoolean()
                }
            }
        }
        return gray!!
    }

    fun addGrayProject(projectId: String, redisOperation: RedisOperation) {
        redisOperation.addSetValue(getGrayRedisKey(), projectId) // 添加项目为灰度项目
        try {
            cache.invalidate(getGrayRedisKey())
        } catch (ignored: Exception) {
        }
    }

    fun removeGrayProject(projectId: String, redisOperation: RedisOperation) {
        redisOperation.removeSetMember(getGrayRedisKey(), projectId) // 取消项目为灰度项目
        try {
            cache.invalidate(getGrayRedisKey())
        } catch (ignored: Exception) {
        }
    }

    fun addCodeCCGrayProject(projectId: String, redisOperation: RedisOperation) {
        redisOperation.addSetValue(getCodeCCGrayRedisKey(), projectId) // 添加项目为灰度项目
        try {
            cache.invalidate(getCodeCCGrayRedisKey())
        } catch (ignored: Exception) {
        }
    }

    fun removeCodeCCGrayProject(projectId: String, redisOperation: RedisOperation) {
        redisOperation.removeSetMember(getCodeCCGrayRedisKey(), projectId) // 取消项目为灰度项目
        try {
            cache.invalidate(getCodeCCGrayRedisKey())
        } catch (ignored: Exception) {
        }
    }

    fun isGrayProject(projectId: String, redisOperation: RedisOperation): Boolean {
        return redisOperation.isMember(getGrayRedisKey(), projectId)
//        return grayProjectSet(redisOperation).contains(projectId)
    }

    fun isGrayMatchProject(projectId: String, redisOperation: RedisOperation): Boolean {
        return isGray() == isGrayProject(projectId, redisOperation) // 当前是灰度环境 + 灰度项目
//        return isGrayMatchProject(projectId, grayProjectSet(redisOperation))
    }

//    fun isGrayMatchProject(projectId: String, grayProjectSet: Set<String>): Boolean {
//        return isGray() == grayProjectSet.contains(projectId)
//    }

    fun grayProjectSet(redisOperation: RedisOperation): Set<String> {
        var projects = cache.getIfPresent(getGrayRedisKey())
        if (projects != null) {
            return projects
        }
        synchronized(this) {
            projects = cache.getIfPresent(getGrayRedisKey())
            if (projects != null) {
                return projects!!
            }
            logger.info("Refresh the local gray projects")
            projects = (redisOperation.getSetMembers(getGrayRedisKey()) ?: emptySet()).filter { !it.isBlank() }.toSet()
            cache.put(getGrayRedisKey(), projects!!)
        }
        return projects!!
    }

    fun getGrayRedisKey() = redisKey

    fun getCodeCCGrayRedisKey() = codeCCRedisKey

    companion object {
        private val logger = LoggerFactory.getLogger(Gray::class.java)
    }
}
