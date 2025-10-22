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
package com.tencent.devops.buildless.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.buildless.pojo.BuildLessPoolInfo
import com.tencent.devops.buildless.pojo.BuildLessTask
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RedisUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {
    /**
     * containerId 12位容器ID
     */
    fun setBuildLessPoolContainer(
        containerId: String,
        containerStatus: ContainerStatus,
        buildLessTask: BuildLessTask? = null
    ) {
        val formatContainerId = CommonUtils.formatContainerId(containerId)
        val buildLessPoolInfo = BuildLessPoolInfo(
            status = containerStatus,
            buildLessTask = buildLessTask
        )
        redisOperation.hset(
            key = buildLessPoolKey(),
            hashKey = formatContainerId,
            values = JsonUtil.toJson(buildLessPoolInfo))
    }

    /**
     * containerId 64位容器ID
     */
    fun deleteBuildLessPoolContainer(containerId: String) {
        logger.info("----> buildLessPoolKey hdelete $containerId")
        redisOperation.hdelete(buildLessPoolKey(), CommonUtils.formatContainerId(containerId))
    }

    fun getBuildLessPoolContainerList(): MutableMap<String, String> {
        return redisOperation.hentries(buildLessPoolKey()) ?: mutableMapOf()
    }

    /**
     * containerId 12位容器ID
     */
    fun getBuildLessPoolContainer(containerId: String): BuildLessPoolInfo? {
        val formatContainerId = CommonUtils.formatContainerId(containerId)
        val result = redisOperation.hget(buildLessPoolKey(), formatContainerId)
        return if (result != null) {
            return objectMapper.readValue(result, BuildLessPoolInfo::class.java)
        } else {
            null
        }
    }

    fun getBuildLessPoolContainerIdle(): Int {
        val values = redisOperation.hvalues(buildLessPoolKey())

        return values?.stream()?.filter {
            val buildLessPoolInfo = objectMapper.readValue(it, BuildLessPoolInfo::class.java)
            buildLessPoolInfo.status == ContainerStatus.IDLE
        }?.count()?.toInt() ?: 0
    }

    fun increIdlePool(incr: Long) {
        redisOperation.increment(idlePoolKey(), incr)
    }

    fun setIdlePool(idleSize: Int) {
        redisOperation.set(idlePoolKey(), idleSize.toString())
    }

    fun getIdlePoolSize(): Long {
        return redisOperation.get(idlePoolKey())?.toLong() ?: 0L
    }

    fun leftPushBuildLessReadyTask(buildLessTask: BuildLessTask) {
        redisOperation.leftPush(buildLessReadyTaskKey(), JsonUtil.toJson(buildLessTask))
    }

    fun rightPushBuildLessReadyTask(buildLessTask: BuildLessTask) {
        redisOperation.rightPush(buildLessReadyTaskKey(), JsonUtil.toJson(buildLessTask))
    }

    fun popBuildLessReadyTask(): BuildLessTask? {
        val resultString = redisOperation.rightPop(buildLessReadyTaskKey())
        if (resultString.isNullOrBlank()) {
            return null
        }

        return JsonUtil.to(resultString, BuildLessTask::class.java)
    }

    fun getBuildLessReadyTaskSize(): Long {
        return redisOperation.listSize(buildLessReadyTaskKey()) ?: 0L
    }

    private fun idlePoolKey(): String {
        return "buildless:idle_pool:${CommonUtils.getHostIp()}"
    }

    private fun buildLessPoolKey(): String {
        return "buildless:contianer_pool:${CommonUtils.getHostIp()}"
    }

    private fun buildLessReadyTaskKey(): String {
        return "buildless:ready_task:${CommonUtils.getHostIp()}"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RedisUtils::class.java)
    }
}
