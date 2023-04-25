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

package com.tencent.devops.auth.service.gitci

import com.google.common.cache.CacheBuilder
import com.tencent.devops.auth.ScmRetryUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.scm.api.ServiceGitCiResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

class GitProjectInfoService @Autowired constructor(
    val client: Client
) {
    private val gitCIUserCache = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_COUNT)
        .expireAfterWrite(USER_CACHE_TIMEOUT, TimeUnit.HOURS)
        .build<String/*userId*/, String>()

    private val projectPublicCache = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_COUNT)
        .expireAfterWrite(PROJECT_CACHE_TIMEOUT, TimeUnit.MINUTES)
        .build<String/*project*/, String?>()

    fun checkProjectPublic(projectCode: String): Boolean {
        if (!projectPublicCache.getIfPresent(projectCode).isNullOrEmpty()) {
            return true
        } else {
            val visibilityLevel = ScmRetryUtils.callScm(0, logger) {
                client.getScm(ServiceGitCiResource::class)
                    .getGitCodeProjectInfo(projectCode).data?.visibilityLevel
            }
            if (visibilityLevel != null) {
                logger.info("project $projectCode visibilityLevel: $visibilityLevel")
                if (visibilityLevel > 0) {
                    projectPublicCache.put(projectCode, visibilityLevel.toString())
                    return true
                }
            } else {
                logger.warn("project $projectCode get projectInfo is empty")
            }
        }
        return false
    }

    fun getGitUserByRtx(rtxUserId: String, projectCode: String): String? {
        return if (!gitCIUserCache.getIfPresent(rtxUserId).isNullOrEmpty()) {
            gitCIUserCache.getIfPresent(rtxUserId)!!
        } else {
            val userId = ScmRetryUtils.callScm(0, logger) {
                client.getScm(ServiceGitCiResource::class).getGitUserId(rtxUserId, projectCode).data
            }
            if (userId != null) {
                gitCIUserCache.put(rtxUserId, userId.toString())
            }
            userId
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(GitProjectInfoService::class.java)
        const val MAX_CACHE_COUNT = 2000L
        const val USER_CACHE_TIMEOUT = 24L
        const val PROJECT_CACHE_TIMEOUT = 5L
    }
}
