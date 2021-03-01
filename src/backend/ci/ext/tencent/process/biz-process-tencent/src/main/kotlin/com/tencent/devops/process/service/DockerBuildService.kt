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

package com.tencent.devops.process.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.pojo.DockerEnableProject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DockerBuildService @Autowired constructor(private val redisOperation: RedisOperation) {

    private val REDIS_DOCKER_BUILD_KEY = "process.projects.docker.build.enable"
    private val REDIS_DOCKER_BUILD_LOCK_KEY = "process.projects.docker.build.lock"

    fun isEnable(userId: String, projectId: String): Boolean {
        val enableProjects = redisOperation.get(REDIS_DOCKER_BUILD_KEY)
        if (enableProjects.isNullOrBlank()) {
            return true
        }
        val projects = parseDockerBuilds(enableProjects)
        if (projects.isEmpty()) {
            return true
        }

        projects.forEach {
            if (it.projectId == projectId) {
                return it.enable
            }
        }

        return true
    }

    fun enable(projectId: String, enable: Boolean) {
        logger.info("Enable the project($projectId) docker build")
        val redisLock = RedisLock(redisOperation, REDIS_DOCKER_BUILD_LOCK_KEY, 60)
        try {
            redisLock.lock()
            val enableProjects = redisOperation.get(REDIS_DOCKER_BUILD_KEY)
            val now = System.currentTimeMillis()
            val projects = if (enableProjects.isNullOrBlank()) {
                listOf(
                        DockerEnableProject(enable, projectId, now, now)
                )
            } else {
                val p = parseDockerBuilds(enableProjects)
                var exist = false
                run lit@{
                    p.forEach {
                        if (it.projectId == projectId) {
                            it.enable = enable
                            it.updateTime = now
                            exist = true
                        }
                    }
                }

                if (!exist) {
                    p.plus(DockerEnableProject(enable, projectId, now, now))
                } else {
                    p
                }
            }

            val projectStr = JsonUtil.getObjectMapper().writeValueAsString(projects)
            logger.info("Update the docker project($projectStr)")
            redisOperation.set(key = REDIS_DOCKER_BUILD_KEY, value = projectStr, expired = false)
        } finally {
            redisLock.unlock()
        }
    }

    fun getAllEnableProjects() =
            parseDockerBuilds(redisOperation.get(REDIS_DOCKER_BUILD_KEY))

    private fun parseDockerBuilds(enableProjects: String?): List<DockerEnableProject> {
        try {
            if (!enableProjects.isNullOrBlank()) {
                return JsonUtil.getObjectMapper().readValue(enableProjects!!)
            }
        } catch (t: Throwable) {
            logger.warn("Fail to parse the docker builds($enableProjects)", t)
        }
        return emptyList()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DockerBuildService::class.java)
    }
}
