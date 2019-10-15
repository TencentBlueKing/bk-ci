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
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.Executors

@Service
class PushImageService @Autowired constructor(
    private val client: Client,
    private val dockerConfig: DockerConfig,
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val rabbitTemplate: RabbitTemplate
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
        logger.info("[${pushImageParam.buildId}]|push image, taskId: ${task.taskId}, pushImageParam: ${pushImageParam.outStr()}")

        val fromImage =
            "${dockerConfig.imagePrefix}/paas/${pushImageParam.projectId}/${pushImageParam.srcImageName}:${pushImageParam.srcImageTag}"
        logger.info("源镜像：$fromImage")
        val toImageRepo = "${pushImageParam.repoAddress}/${pushImageParam.namespace}/${pushImageParam.targetImageName}"
        try {
            pullImage(fromImage)
            logger.info("[${pushImageParam.buildId}]|Pull image success, image name and tag: $fromImage")
            dockerClient.tagImageCmd(fromImage, toImageRepo, pushImageParam.targetImageTag).exec()
            logger.info("[${pushImageParam.buildId}]|Tag image success, image name and tag: $toImageRepo:${pushImageParam.targetImageTag}")
            LogUtils.addLine(
                rabbitTemplate = rabbitTemplate,
                buildId = pushImageParam.buildId,
                message = "目标镜像：$toImageRepo:${pushImageParam.targetImageTag}",
                tag = pushImageParam.buildId,
                executeCount = pushImageParam.executeCount ?: 1
            )
            pushImageToRepo(pushImageParam)
            logger.info("[${pushImageParam.buildId}]|Push image success, image name and tag: $toImageRepo:${pushImageParam.targetImageTag}")

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
                logger.info("[${pushImageParam.buildId}]|Remove local source image success: $toImageRepo:${pushImageParam.targetImageTag}")
            } catch (e: Throwable) {
                logger.error("[${pushImageParam.buildId}]|Docker rmi failed, msg: ${e.message}")
            }
        }
    }

    private fun pullImage(image: String) {
        dockerClient.pullImageCmd(image).exec(PullImageResultCallback()).awaitCompletion()
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
            "${pushImageParam.repoAddress}/${pushImageParam.namespace}/${pushImageParam.targetImageName}:${pushImageParam.targetImageTag}"
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