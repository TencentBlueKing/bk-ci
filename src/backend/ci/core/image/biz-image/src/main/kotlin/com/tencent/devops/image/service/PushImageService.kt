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

package com.tencent.devops.image.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.PullImageResultCallback
import com.github.dockerjava.core.command.PushImageResultCallback
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.image.config.DockerConfig
import com.tencent.devops.image.pojo.PushImageParam
import com.tencent.devops.image.pojo.PushImageTask
import com.tencent.devops.image.pojo.enums.TaskStatus
import com.tencent.devops.image.utils.CommonUtils
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.Executors

@Service@Suppress("ALL")
class PushImageService @Autowired constructor(
    private val client: Client,
    private val dockerConfig: DockerConfig,
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val buildLogPrinter: BuildLogPrinter
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PushImageService::class.java)
        private val executorService = Executors.newFixedThreadPool(20)
    }

    private val dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerHost(dockerConfig.dockerHost)
        .withDockerConfig(dockerConfig.dockerConfig)
        .withApiVersion(dockerConfig.apiVersion)
        .withRegistryUrl(dockerConfig.imagePrefix)
        .withRegistryUsername(dockerConfig.registryUsername)
        .withRegistryPassword(SecurityUtil.decrypt(dockerConfig.registryPassword!!))
        .build()

    private val dockerClient = DockerClientBuilder.getInstance(dockerClientConfig).build()

    fun pushImage(pushImageParam: PushImageParam): PushImageTask? {
        val taskId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        val task = PushImageTask(
            taskId = taskId,
            projectId = pushImageParam.projectId,
            operator = pushImageParam.userId,
            createdTime = now.timestamp(),
            updatedTime = now.timestamp(),
            taskStatus = TaskStatus.RUNNING.name,
            taskMessage = ""
        )
        setRedisTask(taskId, task)
        executorService.execute { syncPushImage(pushImageParam, task) }
        return task
    }

    private fun buildImageTaskKey(taskId: String): String {
        return "image.pushImageTask_$taskId"
    }

    fun getPushImageTask(taskId: String): PushImageTask? {
        val task = redisOperation.get(buildImageTaskKey(taskId)) ?: return null
        try {
            return objectMapper.readValue(task, PushImageTask::class.java)
        } catch (t: Throwable) {
            logger.warn("covert imageTask failed, task: $task", t)
        }
        return null
    }

    private fun setRedisTask(taskId: String, task: PushImageTask) {
        redisOperation.set(buildImageTaskKey(taskId), objectMapper.writeValueAsString(task), 3600)
    }

    private fun syncPushImage(pushImageParam: PushImageParam, task: PushImageTask) {
        logger.info("[${pushImageParam.buildId}]|push image, taskId:" +
            " ${task.taskId}, pushImageParam: ${pushImageParam.outStr()}")

        val fromImage =
            "${dockerConfig.imagePrefix}/paas/${pushImageParam.projectId}/" +
                "${pushImageParam.srcImageName}:${pushImageParam.srcImageTag}"
        logger.info("Source image：$fromImage")
        val toImageRepo = "${pushImageParam.repoAddress}/${pushImageParam.namespace}/${pushImageParam.targetImageName}"
        try {
            pullImage(fromImage)
            logger.info("[${pushImageParam.buildId}]|Pull image success, image name and tag: $fromImage")
            dockerClient.tagImageCmd(fromImage, toImageRepo, pushImageParam.targetImageTag).exec()
            logger.info("[${pushImageParam.buildId}]|Tag image success, image name and tag: " +
                "$toImageRepo:${pushImageParam.targetImageTag}")
            buildLogPrinter.addLine(
                buildId = pushImageParam.buildId,
                message = "Target image：$toImageRepo:${pushImageParam.targetImageTag}",
                tag = pushImageParam.buildId,
                executeCount = pushImageParam.executeCount ?: 1
            )
            pushImageToRepo(pushImageParam)
            logger.info("[${pushImageParam.buildId}]|Push image success, image name and tag:" +
                " $toImageRepo:${pushImageParam.targetImageTag}")

            task.apply {
                taskStatus = TaskStatus.SUCCESS.name
                updatedTime = LocalDateTime.now().timestamp()
            }
            setRedisTask(task.taskId, task)
        } catch (e: Throwable) {
            logger.error("[${pushImageParam.buildId}]|push image error", e)
            task.apply {
                taskStatus = TaskStatus.FAILED.name
                updatedTime = LocalDateTime.now().timestamp()
                taskMessage = e.message!!
            }
            setRedisTask(task.taskId, task)
        } finally {
            try {
                dockerClient.removeImageCmd(fromImage).exec()
                logger.info("[${pushImageParam.buildId}]|Remove local source image success: $fromImage")
            } catch (e: Throwable) {
                logger.error("[${pushImageParam.buildId}]|Docker rmi failed, msg: ${e.message}")
            }
            try {
                dockerClient.removeImageCmd("$toImageRepo:${pushImageParam.targetImageTag}").exec()
                logger.info("[${pushImageParam.buildId}]|Remove local source image success: " +
                    "$toImageRepo:${pushImageParam.targetImageTag}")
            } catch (e: Throwable) {
                logger.error("[${pushImageParam.buildId}]|Docker rmi failed, msg: ${e.message}")
            }
        }
    }

    private fun pullImage(image: String) {
        val authConfig = AuthConfig()
            .withUsername(dockerConfig.registryUsername)
            .withPassword(SecurityUtil.decrypt(dockerConfig.registryPassword!!))
            .withRegistryAddress(dockerConfig.registryUrl)

        dockerClient.pullImageCmd(image).withAuthConfig(authConfig).exec(PullImageResultCallback()).awaitCompletion()
    }

    private fun pushImageToRepo(pushImageParam: PushImageParam) {
        var userName: String? = null
        var password: String? = null
        val ticketId = pushImageParam.ticketId
        if (null != ticketId) {
            val ticketsMap =
                CommonUtils.getCredential(client, pushImageParam.projectId, ticketId, CredentialType.USERNAME_PASSWORD)
            userName = ticketsMap["v1"] as String
            password = ticketsMap["v2"] as String
        }
        val image =
            "${pushImageParam.repoAddress}/${pushImageParam.namespace}/" +
                "${pushImageParam.targetImageName}:${pushImageParam.targetImageTag}"
        val builder = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(dockerConfig.dockerHost)
            .withDockerConfig(dockerConfig.dockerConfig)
            .withApiVersion(dockerConfig.apiVersion)
            .withRegistryUrl(pushImageParam.repoAddress)
        if (null != userName) {
            builder.withRegistryUsername(userName)
        }
        if (null != password) {
            builder.withRegistryPassword(password)
        }
        val dockerClientConfig = builder.build()
        val dockerClient = DockerClientBuilder.getInstance(dockerClientConfig).build()
        try {
            val authConfig = AuthConfig()
                .withUsername(userName)
                .withPassword(password)
                .withRegistryAddress(pushImageParam.repoAddress)
            dockerClient.pushImageCmd(image).withAuthConfig(authConfig).exec(PushImageResultCallback())
                .awaitCompletion()
        } finally {
            try {
                dockerClient.close()
            } catch (e: Exception) {
                logger.error("[${pushImageParam.buildId}]| dockerClient close error:", e)
            }
        }
    }
}
