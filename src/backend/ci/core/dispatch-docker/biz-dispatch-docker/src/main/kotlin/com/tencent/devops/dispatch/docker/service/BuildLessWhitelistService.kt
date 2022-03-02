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

package com.tencent.devops.dispatch.docker.service

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.docker.common.Constants
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BuildLessWhitelistService constructor(
    private val redisOperation: RedisOperation
) {
    private val logger = LoggerFactory.getLogger(BuildLessWhitelistService::class.java)

    fun getDockerResourceWhiteList(userId: String): List<String> {
        val whiteList = mutableListOf<String>()

        val whiteSet = redisOperation.getSetMembers(Constants.BUILD_LESS_WHITE_LIST_KEY_PREFIX)
        return if (whiteSet != null) {
            whiteSet.parallelStream().forEach {
                whiteList.add(it)
            }

            whiteList
        } else {
            emptyList()
        }
    }

    fun addBuildLessWhiteList(userId: String, projectId: String): Boolean {
        redisOperation.addSetValue(Constants.BUILD_LESS_WHITE_LIST_KEY_PREFIX, projectId)
        return true
    }

    fun deleteBuildLessWhiteList(userId: String, projectId: String): Boolean {
        redisOperation.removeSetMember(Constants.BUILD_LESS_WHITE_LIST_KEY_PREFIX, projectId)
        return true
    }

    fun checkBuildLessWhitelist(projectId: String): Boolean {
        return redisOperation.isMember(Constants.BUILD_LESS_WHITE_LIST_KEY_PREFIX, projectId)
    }
}
