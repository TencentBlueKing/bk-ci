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

package com.tencent.devops.process.trigger.scm

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.repository.api.ServiceRepositoryPacResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * scm 灰度策略服务类
 */
@Service
class WebhookGrayService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val client: Client
) {

    private val grayRepoCache = Caffeine.newBuilder()
        .maximumSize(MAX_SIZE)
        .expireAfterAccess(30, TimeUnit.SECONDS)
        .build<String, Boolean?>()

    private val pacGrayRepoCache = Caffeine.newBuilder()
        .maximumSize(MAX_SIZE)
        .expireAfterAccess(30, TimeUnit.SECONDS)
        .build<String, Boolean?>()

    /**
     * 判断是否是scm灰度项目
     */
    fun isGrayRepo(scmCode: String, serverRepoName: String): Boolean {
        // 新的代码库平台,不需要走灰度逻辑,默认直接开启灰度
        if (!needGrayScmCode(scmCode)) {
            return true
        }
        val grayRepoKey = "$scmCode:$serverRepoName"
        return grayRepoCache.getIfPresent(grayRepoKey) ?: run {
            val hash = (serverRepoName.hashCode() and Int.MAX_VALUE) % 100
            val result = when {
                isGrayRepoWhite(scmCode, serverRepoName, false) -> true
                isGrayRepoBlack(scmCode, serverRepoName) -> false
                hash <= getGrayRepoWeight(scmCode).toInt() -> true
                else -> false
            }
            grayRepoCache.put(grayRepoKey, result)
            result
        }
    }

    private fun needGrayScmCode(scmCode: String): Boolean {
        return listOf(
            ScmType.CODE_SVN.name,
            ScmType.CODE_GIT.name,
            ScmType.GITHUB.name,
            ScmType.CODE_TGIT.name,
            ScmType.CODE_P4.name,
            ScmType.CODE_GITLAB.name
        ).contains(scmCode)
    }

    /**
     * 只要开启pac,就执行pac灰度逻辑
     */
    fun isPacGrayRepo(scmTye: ScmType, externalId: String): Boolean {
        val cacheKey = "$scmTye:$externalId"
        return pacGrayRepoCache.getIfPresent(cacheKey) ?: run {
            val result = try {
                client.get(ServiceRepositoryPacResource::class).getPacRepository(
                    externalId = externalId, scmType = scmTye
                ).data != null
            } catch (ignored: Exception) {
                logger.error("Failed to get pac repository|$scmTye|$externalId", ignored)
                false
            }
            pacGrayRepoCache.put(cacheKey, result)
            result
        }
    }

    /**
     * 添加scm灰度项目
     */
    fun addGrayRepoWhite(scmCode: String, serverRepoNames: List<String>, pac: Boolean) {
        serverRepoNames.forEach { serverRepoName ->
            redisOperation.sadd(getGrayRepoWhiteKey(scmCode, pac), serverRepoName)
        }
    }

    /**
     * 移除scm灰度
     */
    fun removeGrayRepoWhite(scmCode: String, serverRepoNames: List<String>, pac: Boolean) {
        serverRepoNames.forEach { serverRepoName ->
            redisOperation.sremove(getGrayRepoWhiteKey(scmCode, pac), serverRepoName)
        }
    }

    fun isGrayRepoWhite(scmCode: String, serverRepoName: String, pac: Boolean): Boolean {
        return redisOperation.isMember(getGrayRepoWhiteKey(scmCode, pac), serverRepoName)
    }

    /**
     * 添加scm灰度黑名单
     */
    fun addGrayRepoBlack(scmCode: String, serverRepoNames: List<String>) {
        serverRepoNames.forEach { serverRepoName ->
            redisOperation.sadd(getGrayRepoBlackKey(scmCode), serverRepoName)
        }
    }

    /**
     * 移除scm灰度黑名单
     */
    fun removeGrayRepoBlack(scmCode: String, serverRepoNames: List<String>) {
        serverRepoNames.forEach { serverRepoName ->
            redisOperation.sremove(getGrayRepoBlackKey(scmCode), serverRepoName)
        }
    }

    fun isGrayRepoBlack(scmCode: String, serverRepoName: String): Boolean {
        return redisOperation.isMember(getGrayRepoBlackKey(scmCode), serverRepoName)
    }

    /**
     * 更新scm灰度权重
     */
    fun updateGrayRepoWeight(scmCode: String, weight: String) {
        redisOperation.set(getGrayRepoWeightKey(scmCode), weight)
    }

    fun getGrayRepoWeight(scmCode: String): String {
        return redisOperation.get(getGrayRepoWeightKey(scmCode)) ?: "-1"
    }

    fun getGrayRepoWhiteKey(scmCode: String, pac: Boolean) = if (pac) {
        "$SCM_PAC_GRAY_WHITE_PREFIX$scmCode"
    } else {
        "$SCM_GRAY_WHITE_PREFIX$scmCode"
    }

    fun getGrayRepoBlackKey(scmCode: String) = "$SCM_GRAY_BLACK_PREFIX$scmCode"

    fun getGrayRepoWeightKey(scmCode: String) = "$SCM_GRAY_WEIGHT_PREFIX$scmCode"

    companion object {
        // scm 灰度白名单,执行灰度逻辑
        private const val SCM_GRAY_WHITE_PREFIX = "process:scm:gray:white:"

        // scm pac灰度白名单,执行灰度逻辑
        private const val SCM_PAC_GRAY_WHITE_PREFIX = "process:scm:pac:gray:white:"

        // scm 灰度黑名单,不能执行灰度逻辑
        private const val SCM_GRAY_BLACK_PREFIX = "process:scm:gray:black:"

        // scm 灰度路由权重,能执行灰度逻辑
        private const val SCM_GRAY_WEIGHT_PREFIX = "process:scm:gray:weight:"

        private const val MAX_SIZE = 5000L

        private val logger = LoggerFactory.getLogger(WebhookGrayService::class.java)
    }
}
