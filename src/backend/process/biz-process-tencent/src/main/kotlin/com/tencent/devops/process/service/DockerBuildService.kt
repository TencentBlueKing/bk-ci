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